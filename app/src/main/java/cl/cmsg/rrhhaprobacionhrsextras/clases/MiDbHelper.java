package cl.cmsg.rrhhaprobacionhrsextras.clases;

/**
 * Created by ocantuarias on 25-05-2016.
 */


        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteOpenHelper;

        import java.text.DateFormat;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Locale;

        import cl.cmsg.rrhhaprobacionhrsextras.R;

/**
 * Created by jarriaran on 20-05-2015.
 */
@SuppressWarnings("SpellCheckingInspection")
public class MiDbHelper extends SQLiteOpenHelper{

    private static MiDbHelper mInstance = null;

    private static int DATABASE_VERSION = 1;
    private static String DATABASE_NAME = "nombre_base_de_datos" ;

    private Context context;

    // Identificacion de tablas


    private String mensajeDeError = "";

    private SQLiteDatabase db;

    public static final String tablaLogErrores = "log_errores";
    public static final String tablaUsuario = "usuario";
    public static final String tablaSolicitud = "solicitud";

    String crearTablaLogErrores =
            "create table " + tablaLogErrores
            + " (id_log_errores integer primary key autoincrement"
            + ",fecha_hora datetime not null"
            + ",version_app varchar(10) not null"
            + ",mac varchar(20) not null"
            + ",descripcion text not null)";

    String crearTablaSolicitud =
            "create table " + tablaSolicitud
                    + " (rut varchar(11) not null"
                    + ",fecha date not null"
                    + ",nombre varchar(50) not null"
                    + ",cant_horas integer not null"
                    + ",monto_pagar integer not null"
                    + ",motivo varchar(250) not null"
                    + ",centro_costo varchar(30) not null"
                    + ",area varchar(30) not null"
                    + ",estado integer not null, primary key (rut, fecha))";

    String crearTablaUsuario =
            "create table " + tablaUsuario
                    + " (rut_u varchar(11) primary key"
                    +",nombre_u varchar(50)  not null)";


    public static MiDbHelper getInstance(Context ctx) {
        /**
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information:
         * http://android-developers.blogspot.nl/2009/01/avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new MiDbHelper(ctx.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * constructor should be private to prevent direct instantiation.
     * make call to static factory method "getInstance()" instead.
     */
    private MiDbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = ctx;
    }

    @Override
// Cuando se crea la base de datos (primera vez que se instala)
    public void onCreate(SQLiteDatabase db){
        db.execSQL(crearTablaLogErrores);
        db.execSQL(crearTablaSolicitud);
        db.execSQL(crearTablaUsuario);


    }

    @Override
// Cuando se actualiza
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
//  db.execSQL("drop table if exist " + tblReporte);
//  db.execSQL("drop table if exist " + tablaReporteImagenes);

//  Se pregunta por la nueva versión por si necesitas algun cambio
        if (newVersion == 2){
            db.execSQL("drop table if exists " + tablaLogErrores);

//   Aqui hay alguna tabla nueva que quieras agregar en una iteración
            db.execSQL("Crear alguna tabla");
        }

//  Otra cosa que se necesite.
        if (newVersion == 3){
            db.execSQL("Crear alguna otra tabla");
        }

    }

    public String getMensajeDeError(){
        return mensajeDeError;
    }

/*
####################################
######## Seccion de consultas ######
####################################
  */

    // Devuelve todos los registros en una variable tipo Cursor
    public Cursor getLogErrores(){
        SQLiteDatabase db = getReadableDatabase();
        return db.query(tablaLogErrores, new String[]{"*"},null, null, null, null, null);
    }

    public String getRutUsuario(){

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(tablaUsuario, new String[]{"*"},null, null, null, null, null);
        while(cursor.moveToNext()){
            return cursor.getString(cursor.getColumnIndex("rut_u"));
        }

        return "";

    }

    public String getNombreUsuario(){

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(tablaUsuario, new String[]{"*"},null, null, null, null, null);
        while(cursor.moveToNext()){
            return cursor.getString(cursor.getColumnIndex("nombre_u"));
        }

        return "";

    }

    public Cursor getDatoSolicitud(String rut, String fecha){

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(tablaSolicitud, new String[]{"*"},"rut=? and fecha=?",new String[]{rut,fecha} , null, null, null);

        return cursor;


    }

    // Borra todos los registros con sus respectivos where
    public boolean deleteLogError(int idLogError){
        SQLiteDatabase db = getReadableDatabase();
        long resultado = -1;
        resultado = db.delete(tablaLogErrores,"id_log_errores=?",new String[]{String.valueOf(idLogError)});
        return (resultado >= 1);
    }

    // Inserta un registro en la tabla
    public boolean insertarLogError(String descripcion){
        db = getWritableDatabase();

        ContentValues campoValor = new ContentValues();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        Date date = new Date();

        campoValor.put("fecha_hora", dateFormat.format(date));
        campoValor.put("descripcion", descripcion);
        campoValor.put("version_app", context.getString(R.string.version));

        long resultado = db.insertOrThrow(tablaLogErrores, null, campoValor);
        return resultado >= 1;
    }

    public boolean insertarUsuario(String rut, String nombre){
        db = getWritableDatabase();

        ContentValues campoValor = new ContentValues();

        campoValor.put("rut_u", rut);
        campoValor.put("nombre_u", nombre);

        long resultado = db.insertOrThrow(tablaUsuario, null, campoValor);
        return resultado >= 1;
    }

    public boolean insertarSolicitud(String rut, String nombre, String fecha, Integer cant_horas, Integer monto_pagar, String motivo,
                                     String centro_costo, String area, Integer estado){
        db = getWritableDatabase();

        ContentValues campoValor = new ContentValues();

        campoValor.put("rut", rut);
        campoValor.put("nombre", nombre);
        campoValor.put("fecha", fecha);
        campoValor.put("cant_horas", cant_horas);
        campoValor.put("monto_pagar", monto_pagar);
        campoValor.put("motivo", motivo);
        campoValor.put("centro_costo", centro_costo);
        campoValor.put("area", area);
        campoValor.put("estado", estado);

        long resultado = db.insertOrThrow(tablaSolicitud, null, campoValor);
        return resultado >= 1;
    }

}
