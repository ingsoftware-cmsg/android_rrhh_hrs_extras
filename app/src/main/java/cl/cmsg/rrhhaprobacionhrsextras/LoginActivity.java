package cl.cmsg.rrhhaprobacionhrsextras;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import cl.cmsg.rrhhaprobacionhrsextras.clases.Alertas;
import cl.cmsg.rrhhaprobacionhrsextras.clases.MiDbHelper;
import cl.cmsg.rrhhaprobacionhrsextras.clases.Rut;
import cl.cmsg.rrhhaprobacionhrsextras.clases.ValidacionConexion;
import cl.cmsg.rrhhaprobacionhrsextras.clases.VolleyS;
import cl.cmsg.rrhhaprobacionhrsextras.gcm.ConstantesGlobales;
import cl.cmsg.rrhhaprobacionhrsextras.gcm.RegistrationIntentService;

public class LoginActivity extends AppCompatActivity{

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 27;
	Button buttonOk;
	MiDbHelper miDbHelper;
	EditText editText;
	ProgressDialog progressDialog;
	String mensaje;
	final String tituloError = "ERROR";
	String tokenRecibido = "";
	Boolean isReceiverRegistered = false;
	BroadcastReceiver mRegistrationBroadcastReceiver;
	VolleyS volleyS;
	String rut;
	String mac;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mac = ValidacionConexion.getDireccionMAC(LoginActivity.this);

		buttonOk = (Button) findViewById(R.id.buttonOk);
		editText = (EditText) findViewById(R.id.editText);
		miDbHelper = MiDbHelper.getInstance(this);

		//Si el usuario existe, envia directamente a MainActivity
		if (!miDbHelper.getRutUsuario().trim().isEmpty()){
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
			return;
		}
		volleyS = VolleyS.getInstance(this);
		progressDialog = new ProgressDialog(LoginActivity.this);
		progressDialog.setTitle("Registrando");
		progressDialog.setMessage("Espere un momento");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);

		mRegistrationBroadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){
				enviarAlServidorCMSG();
			}
		};

		// On Click de boton 'Ok' intenta registrar al usuario
		buttonOk.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View view){
				rut = editText.getText().toString();
				if (!Rut.isRutValido(rut)){
					progressDialog.dismiss();
					Toast.makeText(getApplicationContext(), "rut invalido", Toast.LENGTH_SHORT).show();
					return;
				}
				rut = rut.replace(".", "");
				rut = rut.trim();

				if (!ValidacionConexion.isExisteConexion(LoginActivity.this)){
					Alertas.alertaConexion(LoginActivity.this);
					return;
				}

				progressDialog.show();

				registerReceiver();

				if (checkPlayServices()){
// Start IntentService to register this application with GCM.
					Intent intent = new Intent(LoginActivity.this, RegistrationIntentService.class);
					startService(intent);
				}

			}
		});


	}

	private void registerReceiver(){
		if (!isReceiverRegistered){
			LocalBroadcastManager.getInstance(LoginActivity.this).registerReceiver(
				mRegistrationBroadcastReceiver
				, new IntentFilter(ConstantesGlobales.REGISTRATION_COMPLETE)
			);
			isReceiverRegistered = true;
		}
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices(){
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS){
			if (apiAvailability.isUserResolvableError(resultCode)){
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
					.show();
			} else{
				finish();
			}
			return false;
		}
		return true;
	}


	void enviarAlServidorCMSG(){
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
		tokenRecibido = sharedPreferences.getString(ConstantesGlobales.TOKEN, "");
		if (tokenRecibido.isEmpty()){
			//     No tenemos token, algo pasó y debemos intentarlo nuevamente.
			progressDialog.dismiss();
			desbloquearInterfazUsuario();

			new AlertDialog.Builder(LoginActivity.this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle("Reintentar")
				.setCancelable(false)
				.setMessage(
					"No se pudo reconocer el dispositivo. Por favor, ejecute nuevamente." + "\n"
						+ "La app se cerrará."
				)

				.setPositiveButton("Entendido", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialogInterface, int i){

						finish();
					}
				})
				.show()

			;

			return;
		}

		//     Ya está registrado y guardado el token en shared preferences

		bloquearInterfazUsuario();

		String url = getString(R.string.URL_RegistrarUsuario)
			+ "?apk_key=" + getString(R.string.APK_KEY)
			+ "&run=" + rut
			+ "&mac=" + mac
			+ "&numero_proyecto=" + getString(R.string.num_proyecto)
			+ "&token=" + tokenRecibido;
		StringRequest request = new StringRequest(Request.Method.GET
			, url
			,
			new Response.Listener<String>(){
				@Override
				public void onResponse(String response){
					progressDialog.dismiss();

					String mensajeError = "Comuniquese con informatica, el servidor responde con formato incorrecto";
					JSONObject jsonObject;
					String mensajesrv, nombre;
					int isAdmin = 0;
					desbloquearInterfazUsuario();

					if (response == null || response.equals(null) || response.isEmpty()){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("Variable response es Nulo o Vacio en LoginActivity", mac);
						return;
					}

					try {
						jsonObject = new JSONObject(response);
					} catch (JSONException e){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("Error de formato en variable 'response' en LoginActivity, no parece ser tipo JSON. Mensaje de error : " + e.getMessage(), mac);
						return;
					}

					Boolean error = true;
					try {
						error = jsonObject.getBoolean("error");
					} catch (JSONException e){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("Error de formato en variable 'error' en LoginActivity, No existe o es un formato incorrecto. Mensaje de error : " + e.getMessage(), mac);
						return;
					}

					if (error){
						try {
							mensajesrv = jsonObject.getString("mensaje");
						} catch (JSONException e){
							Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
							miDbHelper.insertarLogError("Error de formato en variable 'mensaje' en LoginActivity, No existe o es un formato incorrecto. Mensaje de error : " + e.getMessage(), mac);
							return;
						}

						new AlertDialog.Builder(LoginActivity.this)
							.setPositiveButton("Ok", null)
							.setCancelable(false)
							.setMessage(mensajesrv)
							.setTitle("Servidor responde con error")
							.show()
						;
						return;
					}

					try {
						nombre = jsonObject.getString("mensaje");
					} catch (JSONException e){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("Error de formato en variable 'mensaje' en LoginActivity, No existe o es un formato incorrecto. Mensaje de error : " + e.getMessage(), mac);
						return;
					}

					try {
						isAdmin = jsonObject.getInt("isadmin");
					} catch (JSONException e){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("Error de formato en variable 'isadmin' en LoginActivity, No existe o es un formato incorrecto. Mensaje de error : " + e.getMessage(), mac);
						return;
					}
					if (isAdmin != 0 && isAdmin != 1){
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("'isadmin' en LoginActivity no es ni 0 ni 1, es: " + isAdmin, mac);
						return;
					}

					miDbHelper.deleteUser();
					rut = rut.replace("-", "").trim();
					rut = rut.substring(0, rut.length() - 1);

					if (miDbHelper.insertarUsuario(rut, nombre, mac, isAdmin)){
						Toast.makeText(LoginActivity.this, "Registrado", Toast.LENGTH_SHORT).show();
						startActivity(new Intent(LoginActivity.this, MainActivity.class));
						finish();
					} else{
						Alertas.alertaSimple(tituloError, mensajeError, LoginActivity.this);
						miDbHelper.insertarLogError("En LoginActivity falló al registrar usuario en la base de datos, el usuario ya existe o mensaje retornó valores incorrectos. Mensaje de error : Una o mas atributos son invalidos", mac);
					}

				}
			}
			,
			new Response.ErrorListener(){
				@Override
				public void onErrorResponse(VolleyError error){
					progressDialog.dismiss();
					desbloquearInterfazUsuario();
					volleyS.cancelAll();
					mensaje = "Servidor no responde \n" +
						" Asegurese de estar conectado a internet o intentelo mas tarde";
					miDbHelper.insertarLogError("Ocurrio un error al comunicarse con el servidor a travez de Volley en LoginActivity, EnviarAlServidorCMSG. Mensaje : " + error.getMessage(), mac);
					Alertas.alertaSimple(tituloError, mensaje, LoginActivity.this);
				}
			}
		);

		volleyS.addToQueue(request, LoginActivity.this);

	}

	private void bloquearInterfazUsuario(){
		buttonOk.setEnabled(false);
		editText.setEnabled(false);
	}

	private void desbloquearInterfazUsuario(){
		buttonOk.setEnabled(true);
		editText.setEnabled(true);
	}


}
