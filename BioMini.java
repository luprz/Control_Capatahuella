package Funciones;

import Clases.horario;
import Formulario.*;
import Formulario.i_Control_ES;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.suprema.ufe33.UFMatcherClass;
import com.suprema.ufe33.UFScannerClass;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BioMini {

    byte[] pImageData;
    private UFScannerClass libScanner = null;
    private UFMatcherClass libMatcher = null;
    private Pointer hMatcher = null;
    byte[] bTemplate = new byte[512];
    int tempsize = 0;
    int[] intTemplateSizeArray = null;
    byte[][] byteTemplateArray = null;
    public final int MAX_TEMPLATE_SIZE = 384;
    private PointerByReference refTemplateArray = null;
    public static String cedulaMarcaje = null;

    public void iniciar() {
        try {

            libScanner = (UFScannerClass) Native.loadLibrary("UFScanner", UFScannerClass.class);
            libMatcher = (UFMatcherClass) Native.loadLibrary("UFMatcher", UFMatcherClass.class);
        } catch (Exception ex) {

            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");
            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");

            return;
        }
        int nRes = 0;

        nRes = libScanner.UFS_Init();
        initArray(100, 1024);
        if (nRes == 0) {

            System.out.println("UFS_Init() success!!");


            System.out.println("UFS_Init() success,nInitFlag value set 1");
            System.out.println("Scanner Init success!!");
        }
    }

    public void initArray(int nArrayCnt, int nMaxTemplateSize) {
        if (byteTemplateArray != null) {
            byteTemplateArray = null;

        }

        if (intTemplateSizeArray != null) {
            intTemplateSizeArray = null;

        }

        byteTemplateArray = new byte[nArrayCnt][MAX_TEMPLATE_SIZE];

        intTemplateSizeArray = new int[nArrayCnt];

        refTemplateArray = new PointerByReference();

    }

    public Pointer GetCurrentScannerHandle() {
        Pointer hScanner = null;
        int nRes = 0;
        int nNumber = 0;
        try {

            libScanner = (UFScannerClass) Native.loadLibrary("UFScanner", UFScannerClass.class);
            libMatcher = (UFMatcherClass) Native.loadLibrary("UFMatcher", UFMatcherClass.class);
        } catch (Exception ex) {

            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");
            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");

        }

        PointerByReference refScanner = new PointerByReference();
        IntByReference refScannerNumber = new IntByReference();

//		�Ʒ� success!!//
        nRes = libScanner.UFS_GetScannerNumber(refScannerNumber);

        if (nRes == 0) {

            nNumber = refScannerNumber.getValue();

            if (nNumber <= 0) {

                return null;
            }

        } else {

            return null;
        }
        nRes = libScanner.UFS_GetScannerHandle(0, refScanner);

        hScanner = refScanner.getValue();

        if (nRes == 0 && hScanner != null) {
            return hScanner;
        }
        return null;
    }

    public void DibujarHuella(ImagePanel huellap) {

        IntByReference refResolution = new IntByReference();
        IntByReference refHeight = new IntByReference();
        IntByReference refWidth = new IntByReference();
        Pointer hScanner = null;

        hScanner = GetCurrentScannerHandle();

        libScanner.UFS_GetCaptureImageBufferInfo(hScanner, refWidth, refHeight, refResolution);

        pImageData = new byte[refWidth.getValue() * refHeight.getValue()];

        libScanner.UFS_GetCaptureImageBuffer(hScanner, pImageData);
        huellap.drawFingerImage(refWidth.getValue(), refHeight.getValue(), pImageData);
    }

    public int CapturaFotoHuella(ImagePanel huellap) {

        int nRes = 0;
        Pointer hScanner = null;

        System.out.println("call GetCurrentScannerHandle()");

        hScanner = GetCurrentScannerHandle();

        if (hScanner != null) {

            System.out.println("GetScannerHandle return hScanner pointer: " + hScanner);

            System.out.println("get Scanner handle success pointer:" + hScanner);

        } else {

            System.out.println("GetScannerHandle fail!!");

            System.out.println("get Scanner handle fail!!");

            return -1;
        }

        System.out.println("Start single image capturing");

        nRes = libScanner.UFS_CaptureSingleImage(hScanner);

        if (nRes == 0) {
            System.out.println("==>UFS_CaptureSingleImage return value is.." + nRes);
            DibujarHuella(huellap);

        } else {

            System.out.println("SingleImage fail!! code:" + nRes);

            byte[] refErr = new byte[512];

            nRes = libScanner.UFS_GetErrorString(nRes, refErr);
            if (nRes == 0) {
                System.out.println("==>UFS_GetErrorString err is " + Native.toString(refErr));
            }

            System.out.println("caputure single img fail!!");
        }

        return nRes;
    }

    public void GuardarHuella(ImagePanel huellap) {
        Pointer hScanner = null;

        hScanner = GetCurrentScannerHandle();

        if (hScanner != null) {

            int nRes = libScanner.UFS_ClearCaptureImageBuffer(hScanner);

            System.out.println("place a finger");

            nRes = libScanner.UFS_CaptureSingleImage(hScanner);

            System.out.println("capture single image");

            if (nRes == 0) {

                //byte[] bTemplate = new byte[512];

                IntByReference refTemplateSize = new IntByReference();

                IntByReference refTemplateQuality = new IntByReference();
                try {
                    nRes = libScanner.UFS_Extract(hScanner, bTemplate, refTemplateSize, refTemplateQuality);
                    if (nRes == 0) {

                        System.out.println("save template file template size:" + refTemplateSize.getValue() + " quality:" + refTemplateQuality.getValue());

                        int nSelectedValue = 50;

                        if (refTemplateQuality.getValue() < nSelectedValue) {
                            System.out.println("template quality < " + nSelectedValue);
                            return;
                        }
                        tempsize = refTemplateSize.getValue();
                        /*System.out.println("A: " + bTemplate);
                        System.out.println("S: " + refTemplateSize.getValue());*/
                        //System.arraycopy(bTemplate, 0, byteTemplateArray[0], 0, refTemplateSize.getValue());
                        /*System.out.println(byteTemplateArray[0]);*/
                        //intTemplateSizeArray[0] = refTemplateSize.getValue();
                        //System.out.println(intTemplateSizeArray[0]);                        
                        DibujarHuella(huellap);
                    } else {
                    }
                } catch (Exception ex) {
                    //MsgBox("exception err:"+ex.getMessage());
                }
            }
        } else {
            // scanner pointer  null	
        }
        /* Conexion c = new Conexion();
        
        c.ingresarHuella(bTemplate, tempsize);*/
    }

    

    public int nuevaHuella(int cedula) {
        int i = 0;
        if (bTemplate != null && tempsize != 0) {
            this.registrarHuella(cedula, bTemplate, tempsize);
            i = 1;
            return i;
        } else {
            return i;
        }
    }

    public void registrarHuella(int cedula, byte[] huella, int tamaño) {
        boolean registrado = false;
        MysqlFunciones conex = new MysqlFunciones();
        Connection con = null;
        try {
            con = conex.ArchivoDB(MysqlFunciones.DB, MysqlFunciones.server, MysqlFunciones.user, MysqlFunciones.pw);
        } catch (IOException ex) {
            Logger.getLogger(BioMini.class.getName()).log(Level.SEVERE, null, ex);
        }
        String Sqlpersonal = "INSERT INTO t_huellas (CEDULA, HUELLA, TAMAÑO) VALUES (?,?,?)";
        try {
            con.setAutoCommit(false);
            try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(Sqlpersonal)) {
                ps.setInt(1, cedula);
                ps.setBytes(2, huella);
                ps.setInt(3, tamaño);
                ps.executeUpdate();
                con.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(BioMini.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void modificarHuella(int cedulaBuscar, int cedula) {
        if (bTemplate != null && tempsize != 0) {
            try {
                this.ActualizarHuella(cedulaBuscar, cedula, bTemplate, tempsize);
            } catch (SQLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static boolean ActualizarHuella(int cedulaBuscar, int cedula, byte[] huella, int tamaño) throws java.sql.SQLException, IOException {
        boolean listo = false;
        String Sqhora_1 = "update t_huellas set CEDULA= ?, HUELLA = ? TAMAÑO=? WHERE CEDULA= ?";
        MysqlFunciones conex = new MysqlFunciones();
        Connection con = conex.ArchivoDB(conex.DB, conex.server, conex.user, conex.pw);
        java.sql.Statement sentencia4 = con.createStatement();
        con.setAutoCommit(false);
        PreparedStatement ps = (PreparedStatement) con.prepareStatement(Sqhora_1);
        ps.setInt(1, cedula);
        ps.setBytes(2, huella);
        ps.setInt(3, tamaño);
        ps.setInt(4, cedulaBuscar);
        ps.executeUpdate();
        con.commit();
        listo = true;
        con.close();
        return listo;
    }

    public void verificar() {
        try {

            libScanner = (UFScannerClass) Native.loadLibrary("UFScanner", UFScannerClass.class);
            libMatcher = (UFMatcherClass) Native.loadLibrary("UFMatcher", UFMatcherClass.class);
        } catch (Exception ex) {

            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");
            System.out.println("loadLlibrary : UFScanner,UFMatcher fail!!");

            return;
        }
        PointerByReference refMatcher = new PointerByReference();
        int nRes = libMatcher.UFM_Create(refMatcher);
        if (nRes == 0) {
            hMatcher = refMatcher.getValue();
        }
        int nSelectedIdx = 0;

        if (nSelectedIdx == -1) {
            System.out.println("selet enroll id");
            return;
        }
        // MsgBox(" enroll id:"+nSelectedIdx + " place a finger");

        Pointer hScanner = null;
        hScanner = GetCurrentScannerHandle();

        if (hScanner == null) {
            System.out.println("getCurrentScannerHandle fail!! ");
            return;
        }

        libScanner.UFS_ClearCaptureImageBuffer(hScanner);

        System.out.println("Place a finger");

        nRes = libScanner.UFS_CaptureSingleImage(hScanner);

        if (nRes != 0) {
            System.out.println("capture single image fail!! " + nRes);
            return;
        }

        byte[] aTemplate = new byte[512];
        PointerByReference refError;
        IntByReference refTemplateSize = new IntByReference();

        IntByReference refTemplateQuality = new IntByReference();

        IntByReference refVerify = new IntByReference();

        nRes = libScanner.UFS_Extract(hScanner, aTemplate, refTemplateSize, refTemplateQuality);
        ResultSet huella;

        if (nRes == 0) {
            try {
                System.out.println("hMat" + hMatcher);
                System.out.println("atemp " + aTemplate);
                System.out.println("size " + refTemplateSize.getValue());
                System.out.println("btemp " + bTemplate);
                System.out.println("size " + tempsize);
                System.out.println("refVery" + refVerify);
                huella = this.buscarHuella();
                int i = 0;
                int id = 0;
                nRes = libMatcher.UFM_Verify(hMatcher, aTemplate, refTemplateSize.getValue(), huella.getBytes(2), huella.getInt(3), refVerify);
                System.out.println("id: " + huella.getInt(1));
                if (refVerify.getValue() == 1) {
                    id = huella.getInt(1);
                } else {
                    while (huella.next()) {
                        System.out.println("id: " + huella.getInt(1));
                        nRes = libMatcher.UFM_Verify(hMatcher, aTemplate, refTemplateSize.getValue(), huella.getBytes(2), huella.getInt(3), refVerify);//byte[][]
                        System.out.println("vuelta: " + i++);
                        if (refVerify.getValue() == 1) {
                            id = huella.getInt(1);
                            break;
                        }
                    }
                }
                if (nRes == 0) {
                    if (refVerify.getValue() == 1) {                        
                        cedulaMarcaje=huella.getString(1);
                        int status2 = horario.Marcar1(huella.getString(1));
                        if (status2 == 3 || status2 == 4) {
                            biblioteca.mensaje("Este empleado se encuentra actualmente inactivo en la empresa");
                        } else {
                            if (status2 == 1 && horario.getC1().before(horario.getC2())) {
                                horario.Marcar2();
                                horario.Marcar1(huella.getString(1));
                                horario.Marcar2();
                                biblioteca.mensaje("No marco la salida en la asistencia anterior");
                                i_Control_ES.DialogAvisoMarcaje.show();
                            } else {
                                horario.Marcar2();
                                i_Control_ES.DialogAvisoMarcaje.show();
                            }
                        }

                    } else {
                        biblioteca.mensaje("Error: este personal no exite en la base de datos.");
                        System.out.println("verify fail!! enroll_id: " + (nSelectedIdx + 1));
                        System.out.println("verify fail!! enroll_id: " + (nSelectedIdx + 1));
                    }
                } else {
                    System.out.println("verify fail!! " + nRes);

                    byte[] refErr = new byte[512];
                    nRes = libMatcher.UFM_GetErrorString(nRes, refErr);
                    if (nRes == 0) {
                        System.out.println("==>UFM_GetErrorString err is " + Native.toString(refErr));
                        System.out.println("==>UFM_GetErrorString err is " + Native.toString(refErr));
                    }

                }
            } catch (Exception ex) {
            }

        } else {
            System.out.println("extract template fail!! " + nRes);

        }
    }

    public ResultSet buscarHuella() throws SQLException {
        String Consulta = "SELECT * from t_huellas";
        ResultSet Resultado1 = null;
        MysqlFunciones conex = new MysqlFunciones();
        Connection con = null;
        try {
            con = conex.ArchivoDB(MysqlFunciones.DB, MysqlFunciones.server, MysqlFunciones.user, MysqlFunciones.pw);
        } catch (IOException ex) {
            Logger.getLogger(BioMini.class.getName()).log(Level.SEVERE, null, ex);
        }
        Statement sentencia3 = (Statement) con.createStatement();
        ResultSet resultado2 = sentencia3.executeQuery(Consulta);

        while (resultado2.next()) {
            Resultado1 = resultado2;
            break;
        }


        return Resultado1;
    }
}
