package sanchezfernandez.franciscojose.tarea12;

import static sanchezfernandez.franciscojose.tarea12.MainActivity.mostrarToast;
import static sanchezfernandez.franciscojose.tarea12.MainActivity.solicitarRecursosAlServer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.regex.Pattern;

/**
 * Con esta clase controlo un AlertDialog personalizado. En el método publico {@link AlertDialogCustom#mostrarDialog(Context)}
 * defino un patrón singleton para garantizar trabajar con la misma instancia y vista asociada.
 */
public class AlertDialogCustom extends AlertDialog {
    private static TextView tvEstado;
    private static Context context;

    private static AlertDialogCustom alert = null;

    private static RadioGroup radioGroup;
    private static RadioButton rbGit;
    private static RadioButton rbCustom;
    private static EditText etIP;
    private static EditText etPath;


    protected AlertDialogCustom(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Con este método controlo la creación del dialog y su información.
     *
     * Este método contiene la inicialización de todos los componentes y sus comportamientos.
     */
    public static void mostrarDialog(Context context) {
        View v;
        AlertDialogCustom alertDialog;
        Button btnAceptar, btnCancelar, btnProbar;

        if (alert == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.alert_dialog_config, null);
            alertDialog = new AlertDialogCustom(context);
            alert = alertDialog;
        } else {
            alertDialog = alert;
            v = alertDialog.getWindow().getDecorView();
        }

        tvEstado = v.findViewById(R.id.tv_estado);
        AlertDialogCustom.modificarEstado(MainActivity.getEstado());
        etIP = v.findViewById(R.id.etIP);
        etPath = v.findViewById(R.id.etPath);
        btnAceptar = v.findViewById(R.id.btnAceptar);
        btnAceptar.setOnClickListener(new View.OnClickListener() {
            /**
             * Antes de acptar los datos introducidos se comprueba su valided con {@link AlertDialogCustom#actualizarDatosConexion()}
             * Si son correctos se llama a {@link MainActivity#solicitarRecursosAlServer()} y se
             * oculta el dialog
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                if (!actualizarDatosConexion()) {
                    MainActivity.mostrarToast("No ha introducido una ip válida", Toast.LENGTH_SHORT);
                } else {
                    solicitarRecursosAlServer();
                    alertDialog.hide();
                }
            }
        });
        btnCancelar = v.findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.hide();
            }
        });

        btnProbar = v.findViewById(R.id.btnProbar);
        btnProbar.setOnClickListener(new View.OnClickListener() {
            /**
             * Compruebo la validez del input del usuario y si son correctos se solicitan los recursos
             * al server
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                if (!actualizarDatosConexion()) {
                    mostrarToast( "No ha introducido una ip válida", Toast.LENGTH_SHORT);
                } else {
                    solicitarRecursosAlServer();
                }
            }
        });

        radioGroup = v.findViewById(R.id.rg_fuente_datos);
        rbCustom = v.findViewById(R.id.rb_custom);
        rbGit = v.findViewById(R.id.rb_git);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            /**
             * En este método controlo la seleccion de los radioButtosn y defino un comportamiento
             * especifico para cada uno de ellos.
             * @param group the group in which the checked radio button has changed
             * @param checkedId the unique identifier of the newly checked radio button
             */
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                /*
                En custom se borra el texto de los editText y se permite al usuario escrigbir sobre ellos
                 */
                if (R.id.rb_custom == checkedId) {
                    etIP.setEnabled(true);
                    etPath.setEnabled(true);
                    etIP.setText("");
                    etPath.setText("");

                } else {
                    /*
                    En Git, se usan los editText para mostrar la diracción y el enlace a git
                     */
                    String urlRecursoGit = "https://alvarogonzalezsotillo.github.io";
                    String pathRecuro = "/apuntes-clase/city.list.json";
                    etIP.setText(urlRecursoGit);
                    etPath.setText(pathRecuro);
                    etIP.setEnabled(false);
                    etPath.setEnabled(false);
                }
            }
        });
        if (!rbCustom.isChecked()) rbGit.setChecked(true);

        alertDialog.setView(v);
        if (!alertDialog.isShowing()) alertDialog.show();
    }

    /**
     * Este método comprueba el input del usuario en los editText
     *
     * @return true si es válido y false si no lo son
     */
    private static boolean actualizarDatosConexion() {
        String newIp = etIP.getText().toString();
        String newPath = etPath.getText().toString();
        Pattern pIP = Pattern.compile("^(https?://)?([0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{2,}))$");

        if (pIP.matcher(newIp).matches()) {
            if(!newIp.startsWith("http")) newIp = "http://" + newIp;
            MainActivity.setIpServer(newIp);
            if(!newPath.startsWith("/")) newPath = "/" + newPath;
            MainActivity.setPath(newPath);
            return true;
        } else {
            Toast toast = Toast.makeText(context, "No ha introducido una ip válida", Toast.LENGTH_SHORT);
            toast.show();
            return false;
        }
    }

    /**
     * Con este método se modifica el la información del estado de conexión.
     * @param estado
     */
    public static void modificarEstado(Estado estado) {
        if(alert == null) return;
        switch (estado) {
            case DATOS_RECIBIDOS:
                tvEstado.setText("Datos recibidos");
                tvEstado.setTextColor(Color.GREEN);
                break;
            case NO_CONECTADO:
                tvEstado.setText("NO CONECTADO");
                tvEstado.setTextColor(Color.RED);
        }
    }

    /**
     * Con este método destruyo el Dialog actual para crear uno nuevo cuando se requiera.
     * Me ha sido necesario porque en el cambio de layout el programa no era capaz de volver a mostrar
     * el dialog ya que existen dos vista xml distintas para cada orientación.
     */
    public static void destruirDialog() {
        alert = null;
    }
    public enum Estado {
        DATOS_RECIBIDOS, NO_CONECTADO
    }

}
