package com.laredo.erp.service;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

/**
 * PlanillaService — FR-050.
 *
 * PROCESAMIENTO DE PLANILLA (en TX ACID):
 *  1. Validar rol (solo Administrador rolId=1 o RRHH rolId=3)  ← en el service, no solo en UI
 *  2. Detectar si ya existe planilla para el período (reproceso)
 *     - Si existe y quien llama NO es Admin → excepción
 *     - Si existe y quien llama ES Admin → asiento de reversa + actualizar estado REPROCESADA
 *  3. Para cada empleado ACTIVO: contar días trabajados, calcular bruta proporcional,
 *     descontar ONP y EsSalud (porcentajes desde tabla configuracion)
 *  4. INSERT planilla + detalles
 *  5. Asiento de devengo (Gasto Planilla / CxP Trabajadores)
 *  6. Auditoría
 *
 * REGISTRO DE ASISTENCIA (cálculo automático de estado):
 *  - PUNTUAL si horaEntradaReal <= horaEntrada + tolerancia
 *  - TARDANZA si horaEntradaReal > horaEntrada + tolerancia
 *  - FALTA_JUSTIFICADA / FALTA_INJUSTIFICADA: manual (sin detección de feriados)
 */
public class PlanillaService {

    private static final int ROL_ADMIN = 1;
    private static final int ROL_RRHH  = 3;

    // IDs plan_cuentas (verificar contra BD)
    private static final int CTA_GASTO_PLANILLA  = 9;  // Gasto por Planilla
    private static final int CTA_CXP_TRABAJADORES = 10; // CxP Trabajadores

    private final EmpleadoDAO empleadoDAO        = new EmpleadoDAO();
    private final AsistenciaDAO asistenciaDAO    = new AsistenciaDAO();
    private final PlanillaDAO planillaDAO        = new PlanillaDAO();
    private final AsientoContableDAO asientoDAO  = new AsientoContableDAO();
    private final ConfiguracionDAO configDAO     = new ConfiguracionDAO();
    private final AuditoriaDAO auditoriaDAO      = new AuditoriaDAO();

    // ── Asistencia ────────────────────────────────────────────────────────────

    /**
     * Registra asistencia diaria de un empleado.
     * Calcula automáticamente PUNTUAL vs TARDANZA comparando
     * horaEntradaReal con el horario configurado + tolerancia.
     * FALTA_JUSTIFICADA / FALTA_INJUSTIFICADA se pasan directamente
     * cuando no hay hora de entrada (marcado manual).
     */
    public void registrarAsistencia(Asistencia a, Horario horario) throws VentaException, SQLException {
        if (asistenciaDAO.existeRegistro(a.getEmpleadoId(), a.getFecha())) {
            throw new VentaException("Ya existe registro de asistencia para este empleado en la fecha "
                    + a.getFecha() + ".");
        }

        // Si se registra hora de entrada, calcular estado automáticamente
        if (a.getHoraEntradaReal() != null &&
            (a.getEstado() == Asistencia.Estado.PUNTUAL || a.getEstado() == Asistencia.Estado.TARDANZA)) {
            LocalTime limiteConTolerancia = horario.getHoraEntrada()
                    .plusMinutes(horario.getToleranciaMinutos());
            a.setEstado(a.getHoraEntradaReal().isAfter(limiteConTolerancia)
                    ? Asistencia.Estado.TARDANZA
                    : Asistencia.Estado.PUNTUAL);
        }
        // Si es FALTA_*, se respeta el estado que viene (manual del Administrador/RRHH)

        asistenciaDAO.insertar(a);
    }

    // ── Planilla ──────────────────────────────────────────────────────────────

    /**
     * Procesa la planilla del período dado.
     *
     * @param periodo    "YYYY-MM"
     * @param usuarioId  quien ejecuta
     * @param rolId      rol del usuario (validado aquí, no solo en UI)
     * @param diasMes    días laborales del mes (tomado de configuracion)
     */
    public int procesarPlanilla(String periodo, int usuarioId, int rolId)
            throws VentaException, SQLException {

        // ── PASO 1: Validar rol ──────────────────────────────────────────────
        if (rolId != ROL_ADMIN && rolId != ROL_RRHH) {
            throw new VentaException("Solo el Administrador o el perfil RRHH pueden procesar la planilla.");
        }

        // ── PASO 2: Detectar reproceso ───────────────────────────────────────
        var planillaExistente = planillaDAO.buscarPorPeriodo(periodo);
        if (planillaExistente.isPresent()) {
            if (rolId != ROL_ADMIN) {
                throw new VentaException("El período " + periodo + " ya fue procesado. "
                        + "Solo el Administrador puede autorizar un reproceso.");
            }
            // Administrador autoriza reproceso → asiento de reversa
            generarAsientoReversa(planillaExistente.get(), usuarioId);
        }

        // ── PASO 3: Leer porcentajes desde configuracion ─────────────────────
        BigDecimal pctONP     = leerPorcentaje("ONP_PORCENTAJE",     new BigDecimal("0.13"));
        BigDecimal pctEsSalud = leerPorcentaje("ESSALUD_PORCENTAJE", new BigDecimal("0.09"));
        int diasMes           = leerEntero("DIAS_LABORALES_MES", 30);

        // ── PASO 4: Calcular y acumular detalles ─────────────────────────────
        List<Empleado> empleados = empleadoDAO.listarActivos();
        if (empleados.isEmpty()) {
            throw new VentaException("No hay empleados activos para procesar en el período " + periodo + ".");
        }

        BigDecimal totalNeto = BigDecimal.ZERO;
        Planilla planilla = new Planilla();
        planilla.setPeriodo(periodo);
        planilla.setEstado(planillaExistente.isPresent() ? Planilla.Estado.REPROCESADA : Planilla.Estado.PROCESADA);
        planilla.setUsuarioId(usuarioId);

        for (Empleado emp : empleados) {
            int diasTrabajados = asistenciaDAO.contarDiasTrabajados(emp.getId(), periodo);
            if (diasTrabajados == 0) diasTrabajados = diasMes; // sin asistencia registrada = mes completo (fallback)

            // Remuneración bruta proporcional
            BigDecimal bruta = emp.getRemuneracionBase()
                    .multiply(BigDecimal.valueOf(diasTrabajados))
                    .divide(BigDecimal.valueOf(diasMes), 2, RoundingMode.HALF_UP);

            BigDecimal onp     = bruta.multiply(pctONP).setScale(2, RoundingMode.HALF_UP);
            BigDecimal essalud = bruta.multiply(pctEsSalud).setScale(2, RoundingMode.HALF_UP);
            BigDecimal neta    = bruta.subtract(onp).subtract(essalud).setScale(2, RoundingMode.HALF_UP);

            DetallePlanilla det = new DetallePlanilla();
            det.setEmpleadoId(emp.getId());
            det.setDiasTrabajados(diasTrabajados);
            det.setRemuneracionBruta(bruta);
            det.setOnp(onp);
            det.setEssalud(essalud);
            det.setRemuneracionNeta(neta);
            det.setEmpleadoNombre(emp.getNombreCompleto());
            det.setEmpleadoDni(emp.getDni());
            det.setEmpleadoCargo(emp.getCargo());
            planilla.getDetalles().add(det);
            totalNeto = totalNeto.add(neta);
        }
        planilla.setTotalNeto(totalNeto);

        // ── PASO 5: Persistir en TX ACID ────────────────────────────────────
        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            // Si reproceso, eliminar detalle anterior (el UNIQUE en periodo impide doble INSERT)
            if (planillaExistente.isPresent()) {
                con.prepareStatement("DELETE FROM detalle_planilla WHERE planilla_id = "
                        + planillaExistente.get().getId()).executeUpdate();
                con.prepareStatement("DELETE FROM planillas WHERE id = "
                        + planillaExistente.get().getId()).executeUpdate();
            }

            int planillaId = planillaDAO.insertarPlanilla(planilla, con);
            planilla.setId(planillaId);
            for (DetallePlanilla det : planilla.getDetalles()) {
                det.setPlanillaId(planillaId);
                planillaDAO.insertarDetalle(det, con);
            }

            // Asiento de devengo: DEBE Gasto Planilla, HABER CxP Trabajadores
            AsientoContable asiento = new AsientoContable();
            asiento.setDescripcion("Devengo planilla " + periodo);
            asiento.setReferenciaTipo("PLANILLA");
            asiento.setReferenciaId(planillaId);
            asiento.getDetalles().add(DetalleAsiento.debe(CTA_GASTO_PLANILLA, totalNeto));
            asiento.getDetalles().add(DetalleAsiento.haber(CTA_CXP_TRABAJADORES, totalNeto));
            asientoDAO.insertarCompleto(asiento, con);

            con.commit();

            // Auditoría (fuera de TX)
            auditoriaDAO.registrar(usuarioId, Auditoria.PROCESAMIENTO_PLANILLA,
                    "PLANILLA", planillaId,
                    "Período: " + periodo + " | Total neto: S/ " + totalNeto
                    + (planillaExistente.isPresent() ? " | REPROCESO autorizado por Admin" : ""));

            return planillaId;
        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al procesar planilla — todo revertido: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    private void generarAsientoReversa(Planilla planillaAnterior, int usuarioId) throws SQLException {
        // Buscar detalle para obtener total
        var detalles = planillaDAO.listarDetalle(planillaAnterior.getId());
        BigDecimal totalAnterior = detalles.stream()
                .map(DetallePlanilla::getRemuneracionNeta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AsientoContable reversa = new AsientoContable();
        reversa.setDescripcion("REVERSA planilla " + planillaAnterior.getPeriodo()
                + " (reproceso autorizado)");
        reversa.setReferenciaTipo("PLANILLA_REVERSA");
        reversa.setReferenciaId(planillaAnterior.getId());
        reversa.getDetalles().add(DetalleAsiento.haber(CTA_GASTO_PLANILLA, totalAnterior));
        reversa.getDetalles().add(DetalleAsiento.debe(CTA_CXP_TRABAJADORES, totalAnterior));

        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit(); con.setAutoCommit(false);
        try {
            asientoDAO.insertarCompleto(reversa, con);
            con.commit();
        } catch (Exception e) { con.rollback(); throw e; } finally { con.setAutoCommit(ac); }
    }

    private BigDecimal leerPorcentaje(String clave, BigDecimal defecto) {
        try {
            return configDAO.obtenerValor(clave)
                    .map(v -> new BigDecimal(v).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .orElse(defecto);
        } catch (Exception e) { return defecto; }
    }

    private int leerEntero(String clave, int defecto) {
        try {
            return configDAO.obtenerValor(clave).map(Integer::parseInt).orElse(defecto);
        } catch (Exception e) { return defecto; }
    }
}
