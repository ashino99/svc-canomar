package pe.canomar.svc.servicio.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.CompraDao;
import pe.canomar.svc.dao.CuadreDao;
import pe.canomar.svc.dao.FacturaDao;
import pe.canomar.svc.dao.GastoDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.CierreMensual;
import pe.canomar.svc.modelo.EstadoCuenta;
import pe.canomar.svc.modelo.GananciaProducto;
import pe.canomar.svc.modelo.Movimiento;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.servicio.ReporteService;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Reportes del negocio. Excel con Apache POI (RF48) y PDF del estado de cuenta con OpenPDF (RF37). */
@Service
public class ReporteServiceImpl implements ReporteService {

    private final VentaDao ventaDao;
    private final CompraDao compraDao;
    private final GastoDao gastoDao;
    private final LoteDao loteDao;
    private final FacturaDao facturaDao;
    private final CuadreDao cuadreDao;

    public ReporteServiceImpl(VentaDao ventaDao, CompraDao compraDao, GastoDao gastoDao, LoteDao loteDao, FacturaDao facturaDao, CuadreDao cuadreDao) {
        this.ventaDao = ventaDao;
        this.compraDao = compraDao;
        this.gastoDao = gastoDao;
        this.loteDao = loteDao;
        this.facturaDao = facturaDao;
        this.cuadreDao = cuadreDao;
    }

    @Override
    public CierreMensual cierreMensual(YearMonth mes) {
        LocalDate desde = mes.atDay(1);
        LocalDate hasta = mes.atEndOfMonth();
        LocalDate hoy = Fechas.hoy();
        if (desde.isAfter(hoy)) {
            throw new NegocioException("El mes aún no empieza");
        }
        int sinCuadre = 0;
        LocalDate ultimo = hasta.isBefore(hoy) ? hasta : hoy.minusDays(1);
        for (LocalDate d = desde; !d.isAfter(ultimo); d = d.plusDays(1)) {
            if (ventaDao.cantidadVentas(d, d) > 0 && cuadreDao.buscar(d).isEmpty()) {
                sinCuadre++;
            }
        }
        return new CierreMensual(mes, ventaDao.totalVentas(desde, hasta), ventaDao.costoVendido(desde, hasta), compraDao.totalCompras(desde, hasta),
                gastoDao.total(desde, hasta), loteDao.perdidas(desde, hasta), facturaDao.totalEmitido(desde, hasta), sinCuadre, !hasta.isBefore(hoy));
    }

    @Override
    public List<GananciaProducto> gananciaPorProducto(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Revise el rango de fechas");
        }
        return ventaDao.gananciaPorProducto(desde, hasta);
    }

    @Override
    public byte[] excelCierreMensual(YearMonth mes) {
        CierreMensual c = cierreMensual(mes);
        try (Workbook wb = new XSSFWorkbook()) {
            Estilos e = new Estilos(wb);
            Sheet sh = wb.createSheet("Cierre " + mes);
            titulo(sh, e, "Cierre mensual " + mes + (c.parcial() ? " (parcial)" : "") + " — Canomar");
            Object[][] filas = {
                    {"Ventas del mes", c.ventas()}, {"Costo de lo vendido", c.costoVendido()}, {"Autoconsumo, mermas y desechos", c.perdidas()},
                    {"Gastos menores", c.gastos()}, {"Ganancia del mes", c.ganancia()}, {"Compras del mes", c.compras()},
                    {"Facturado (facturas registradas)", c.facturado()}, {"Ventas sin factura", c.noFacturado()}, {"Días con ventas sin cuadre", c.diasSinCuadre()}};
            encabezado(sh, e, 2, "Concepto", "Monto (S/)");
            int r = 3;
            for (Object[] f : filas) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue((String) f[0]);
                celda(row, 1, f[1], e);
            }
            sh.setColumnWidth(0, 40 * 256);
            sh.setColumnWidth(1, 18 * 256);
            List<GananciaProducto> gp = ventaDao.gananciaPorProducto(mes.atDay(1), mes.atEndOfMonth());
            hojaGananciaProducto(wb, e, gp, "Por producto");
            return bytes(wb);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    @Override
    public byte[] excelGananciaProducto(LocalDate desde, LocalDate hasta) {
        List<GananciaProducto> gp = gananciaPorProducto(desde, hasta);
        try (Workbook wb = new XSSFWorkbook()) {
            hojaGananciaProducto(wb, new Estilos(wb), gp, "Ganancia por producto");
            return bytes(wb);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    @Override
    public byte[] excelVentas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Revise el rango de fechas");
        }
        List<VentaVista> ventas = ventaDao.listarEntre(desde, hasta);
        try (Workbook wb = new XSSFWorkbook()) {
            Estilos e = new Estilos(wb);
            Sheet sh = wb.createSheet("Ventas");
            titulo(sh, e, "Ventas del " + desde.format(Fechas.DIA) + " al " + hasta.format(Fechas.DIA));
            encabezado(sh, e, 2, "N°", "Fecha y hora", "Cliente", "Tipo", "Medio", "Remate", "Estado", "Factura", "Total (S/)");
            int r = 3;
            for (VentaVista v : ventas) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(v.numero());
                row.createCell(1).setCellValue(v.fechaHora().format(Fechas.DIA_HORA));
                row.createCell(2).setCellValue(v.cliente());
                row.createCell(3).setCellValue(v.tipoPago());
                row.createCell(4).setCellValue(v.medioPago() == null ? "" : v.medioPago());
                row.createCell(5).setCellValue(v.esRemate() ? "Sí" : "No");
                row.createCell(6).setCellValue(v.estado());
                row.createCell(7).setCellValue(v.idFactura() == null ? "" : "Sí");
                celda(row, 8, v.total(), e);
            }
            int[] anchos = {8, 18, 30, 10, 14, 8, 10, 8, 14};
            for (int i = 0; i < anchos.length; i++) {
                sh.setColumnWidth(i, anchos[i] * 256);
            }
            return bytes(wb);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    @Override
    public byte[] pdfEstadoCuenta(EstadoCuenta ec) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font tit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(14, 58, 91));
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            doc.add(new Paragraph("Canomar — Estado de cuenta", tit));
            doc.add(new Paragraph("Terminal Pesquero de Villa María del Triunfo", normal));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Cliente: " + ec.cliente().nombreComercial() + (ec.cliente().ruc() != null ? "   RUC: " + ec.cliente().ruc() : ""), negrita));
            doc.add(new Paragraph("Periodo: " + ec.desde().format(Fechas.DIA) + " al " + ec.hasta().format(Fechas.DIA), normal));
            doc.add(new Paragraph(" "));
            PdfPTable t = new PdfPTable(5);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{2.2f, 4f, 1.6f, 1.6f, 1.8f});
            for (String h : new String[]{"Fecha", "Descripción", "Cargo", "Abono", "Saldo"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, negrita));
                c.setBackgroundColor(new Color(217, 226, 243));
                t.addCell(c);
            }
            t.addCell(new Phrase("", normal));
            t.addCell(new Phrase("Saldo inicial", negrita));
            t.addCell(new Phrase("", normal));
            t.addCell(new Phrase("", normal));
            t.addCell(numero(ec.saldoInicial(), negrita));
            for (Movimiento m : ec.movimientos()) {
                t.addCell(new Phrase(m.fecha().format(Fechas.DIA_HORA), normal));
                t.addCell(new Phrase(m.descripcion(), normal));
                t.addCell(numero(m.cargo(), normal));
                t.addCell(numero(m.abono(), normal));
                t.addCell(numero(m.saldo(), normal));
            }
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Saldo al " + ec.hasta().format(Fechas.DIA) + ": S/ " + ec.saldoFinal(), tit));
            doc.add(new Paragraph("Generado el " + Fechas.ahora().format(Fechas.DIA_HORA) + " con SVC-Canomar.", normal));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    // ---------- utilidades de Excel ----------

    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle encabezado;
        final CellStyle dinero;

        Estilos(Workbook wb) {
            org.apache.poi.ss.usermodel.Font ft = wb.createFont();
            ft.setBold(true);
            ft.setFontHeightInPoints((short) 13);
            titulo = wb.createCellStyle();
            titulo.setFont(ft);
            org.apache.poi.ss.usermodel.Font fb = wb.createFont();
            fb.setBold(true);
            encabezado = wb.createCellStyle();
            encabezado.setFont(fb);
            encabezado.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dinero = wb.createCellStyle();
            dinero.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        }
    }

    private void hojaGananciaProducto(Workbook wb, Estilos e, List<GananciaProducto> gp, String nombre) {
        Sheet sh = wb.createSheet(nombre);
        titulo(sh, e, "Ganancia por producto");
        encabezado(sh, e, 2, "Producto", "Kilos", "Ventas (S/)", "Costo (S/)", "Ganancia (S/)", "Margen %");
        int r = 3;
        for (GananciaProducto g : gp) {
            Row row = sh.createRow(r++);
            row.createCell(0).setCellValue(g.producto());
            celda(row, 1, g.kilos(), e);
            celda(row, 2, g.ventas(), e);
            celda(row, 3, g.costo(), e);
            celda(row, 4, g.ganancia(), e);
            celda(row, 5, g.margen(), e);
        }
        sh.setColumnWidth(0, 22 * 256);
        for (int i = 1; i <= 5; i++) {
            sh.setColumnWidth(i, 15 * 256);
        }
    }

    private void titulo(Sheet sh, Estilos e, String texto) {
        Cell c = sh.createRow(0).createCell(0);
        c.setCellValue(texto);
        c.setCellStyle(e.titulo);
    }

    private void encabezado(Sheet sh, Estilos e, int fila, String... columnas) {
        Row row = sh.createRow(fila);
        for (int i = 0; i < columnas.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(columnas[i]);
            c.setCellStyle(e.encabezado);
        }
    }

    private void celda(Row row, int col, Object valor, Estilos e) {
        Cell c = row.createCell(col);
        if (valor instanceof BigDecimal b) {
            c.setCellValue(Dinero.nz(b).doubleValue());
            c.setCellStyle(e.dinero);
        } else if (valor instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(String.valueOf(valor));
        }
    }

    private byte[] bytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private PdfPCell numero(BigDecimal v, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(v == null ? "" : Dinero.r2(v).toPlainString(), f));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
}
