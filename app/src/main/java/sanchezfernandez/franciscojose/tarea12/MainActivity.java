package sanchezfernandez.franciscojose.tarea12;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private static String ipServer = "https://alvarogonzalezsotillo.github.io";
    private static String path = "/apuntes-clase/city.list.json";
    private static AlertDialogCustom.Estado estado = AlertDialogCustom.Estado.NO_CONECTADO;

    public static AlertDialogCustom.Estado getEstado() {
        return estado;
    }

    private static Toast toast;
    private static HashMap<String, Ciudad> ciudades;
    private SearchView svBuscar;
    private ImageView ivConfig;
    private TextView tvNombre, tvPais, tvLatitud, tvLongitud;
    private Button btnBuscar, btnMostrarMapa;
    private AdapterSearch adapterSearch;
    private Ciudad selectedCity;
    private static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarComponentes();
        solicitarRecursosAlServer();
        configurarSearchView();
    }

    /**
     * Este método inicializa todas las view y variables necesarias para el funcionamiento de la app.
     * Define los comportamientos de la mayoria de views excepto del SearchView
     */
    private void inicializarComponentes() {
        ciudades = new HashMap<>();
        context = this;
        btnBuscar = findViewById(R.id.btn_buscar);
        tvNombre = findViewById(R.id.tv_nombre_poblacion);
        tvPais = findViewById(R.id.tv_codigo_pais);
        tvLatitud = findViewById(R.id.tv_latitud);
        tvLongitud = findViewById(R.id.tv_longitud);
        btnMostrarMapa = findViewById(R.id.btn_mapa);
        btnMostrarMapa.setEnabled(false);
        btnMostrarMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCity == null) {
                    mostrarToast("No hay ninguna ciudad seleccionada", Toast.LENGTH_SHORT);
                    return;
                }
                Intent i = new Intent(getApplicationContext(), MapsActivity.class);
                i.putExtra("city", selectedCity);
                startActivity(i);
            }
        });

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svBuscar.getQuery().toString().isEmpty()) {
                    mostrarToast("Tiene que introducir el nombre de la ciudad", Toast.LENGTH_SHORT);
                    return;
                }
                if (ciudades.containsKey(svBuscar.getQuery().toString().toLowerCase())) {
                    Ciudad c = ciudades.get(svBuscar.getQuery().toString().toLowerCase());
                    selectedCity = c;
                    assert c != null;
                    tvNombre.setText(c.getName());
                    tvPais.setText(c.getCountry());
                    tvLatitud.setText(String.valueOf(c.getCoord().getLat()));
                    tvLongitud.setText(String.valueOf(c.getCoord().getLon()));
                    btnMostrarMapa.setEnabled(true);
                    ocultarTeclado();

                } else {
                    mostrarToast("No se ha encontrado ninguna coincidencia", Toast.LENGTH_SHORT);
                }
            }
        });

        ivConfig = findViewById(R.id.iv_config);
        ivConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialog();
            }
        });
    }


    /**
     * Este método usa Volley para conectarse con el servidor y solicitarle el recurso.
     */
    public static void solicitarRecursosAlServer() {
        mostrarToast("Estableciendo conexión con el servidor.\nPor favor, espere", Toast.LENGTH_LONG);
        Log.i("INFO", "Estableciendo conexión con el servidor. URL: " + ipServer + path);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                ipServer + path,
                null,
                new Response.Listener<JSONArray>() {
                    /**
                     * Este método se ejecuta con la respuesa del server. Al ser un Array, se recorre el array
                     * y se crea una instacia Ciudad por cada objeto que contenga el array y se añade al
                     * ArrayList ciudades.
                     * Al finalizar, se notifica al adapter la modificación del dataSet
                     * @param response
                     */
                    @Override
                    public void onResponse(JSONArray response) {
                        if (toast.getDuration() > 0)
                            toast.cancel(); // Si se establece la conexión cancelo el Toast

                        try {
                            Log.i("INFO", "Conexión establecida correctamente");
                            JSONObject object;
                            Ciudad c;
                            for (int i = 0; i < response.length(); i++) {
                                object = response.getJSONObject(i);
                                c = new Ciudad();
                                c.setId(object.getInt("id"));
                                c.setName(object.getString("name"));
                                c.setCountry(object.getString("country"));
                                object = object.getJSONObject("coord");
                                c.setCoord(new Coord(object.getDouble("lon"), object.getDouble("lat")));
                                ciudades.put(c.getName().toLowerCase(), c);
                            }
                            Log.i("INFO", "Datos cargados correctamente.");
                            estado = AlertDialogCustom.Estado.DATOS_RECIBIDOS;
                            AlertDialogCustom.modificarEstado(AlertDialogCustom.Estado.DATOS_RECIBIDOS);
                        } catch (JSONException e) {
                            Log.e("ERROR", "Se ha producido un error durante el parseo de los datos.");
                            estado = AlertDialogCustom.Estado.NO_CONECTADO;
                            mostrarDialog();
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            /**
             * Este método se ejecuta si hay algún error en la conexión. Se informa al usuario y se
             * @param error
             */
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("ERROR", "Se ha producido un error en la repuesta del servidor.");
                mostrarToast("Error al establecer la conexión", Toast.LENGTH_LONG);
                estado = AlertDialogCustom.Estado.NO_CONECTADO;
                mostrarDialog();
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonArrayRequest);
    }

    /**
     * Este método configura el SearchView. He conseguido implementar lo que quería salvo que se muestre
     * la lista de sugerencias nada más tener el foco. He probado sobreescribiendo el comportamiento
     * de varios métodos, pero no he conseguido hacerlo. Tampoco encontré nada por internet y la IA
     * no ha sido capaz de decirme cómo.
     * <p>
     * El SearchView tiene asociado un adapter {@link AdapterSearch} que contiene un Cursor con la
     * lista de ciudades disponibles, de manera que al escribir, el SearchView mostrará como sugerencia
     * las coincidencias. Las sugerencias se muestran a partir de dos letras, yo quería que las mostrara
     * desde que tuviera el foco, para tener una lista completa de ciudades disponibles.
     */
    private void configurarSearchView() {
        svBuscar = findViewById(R.id.sv_input_ciudad);
        svBuscar.setQueryHint("Buscar ciudad");
        svBuscar.setIconified(false);
        svBuscar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /*
            Este método se llama al dar al enter al teclado. Hace la misma función que el botón buscar:
            si existe la ciudad se muestran sus datos en la app, sino existe no se hace nada
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (ciudades.containsKey(query)) {
                    modificarInfo(ciudades.get(query));
                    selectedCity = ciudades.get(query);
                    btnMostrarMapa.setEnabled(true);
                    return true;
                }
                return false;
            }

            /**
             * En este método se controla los cambios en el text. En caso de que este vacío, el programa
             * lo entendrá como que no hay ninguna ciudad selecionada y se borrarán la información que
             * se estuviera mostrando, además de desactivar el btnMostrarMapa
             *
             * Por último, se pasa el texto introducido a {@link MainActivity#filtrarCiudades(String)}
             * @param newText the new content of the query text field.
             *
             * @return true para indicar que el comportamiento está controlado
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    tvNombre.setText("");
                    tvPais.setText("");
                    tvLatitud.setText("");
                    tvLongitud.setText("");
                    btnMostrarMapa.setEnabled(false);
                    selectedCity = null;
                }
                filtrarCiudades(newText);
                return true;
            }
        });
        svBuscar.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            /**
             * Con ese método obtendo la sugerencia elegida por el usuario. Escribo el nombre de la
             * ciudad en el SerachView y llamo al controlador click del btnBuscar para que muestre
             * la info de la ciudad
             * @param position the absolute position of the clicked item in the list of suggestions.
             *
             * @return true
             */
            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = svBuscar.getSuggestionsAdapter().getCursor();
                cursor.moveToPosition(position);
                String ciudad = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                svBuscar.setQuery(ciudad, false);
                btnBuscar.callOnClick();
                ocultarTeclado();
                return true;
            }
        });
        svBuscar.setOnCloseListener(new SearchView.OnCloseListener() {
            /**
             * Con esta sobreescritura evito que se cierre el SearchView (ponerse en modo icono)
             * y además borro la entrada de texto que haya. También desactivo el botón de mostrarMapa
             * @return true
             */
            @Override
            public boolean onClose() {
                svBuscar.setQuery("", false);
                svBuscar.setQueryHint("Buscar ciudad");
                btnMostrarMapa.setEnabled(false);
                selectedCity = null;
                /*
                En el modo land, el serachview puede estar en modo icono. Retornando false, este método
                hace tb sus acciones predeterminadas: borra el texto y si no hay texto lo coloca en modo icono
                 */
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                    return false;
                return true;
            }
        });

        adapterSearch = new AdapterSearch(this, null);

        /*
        Para poder realizar las sugerencias, el SearchView realiza consultas a un cursor.
        Aquí creo el cursor sobre el que luego se harán las consultas. Este cursor obligatoriamente
        debe llevar una columna "_id" que, en este caso, toma el valor de id de la ciudad. La columna
        "name" toma como valor el nombre.
         */
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "name"});
        for (Ciudad c : ciudades.values()) {
            matrixCursor.addRow(new String[]{String.valueOf(c.getId()), c.getName()});
        }
        // Se le pasa el cursor al adapter y el adapter al SearchView
        adapterSearch.swapCursor(matrixCursor);
        svBuscar.setSuggestionsAdapter(adapterSearch);

        /*
        He intentado de mil maneras personalizar todos los colores del SearchView, pero no lo he consegido.
        He conseguido modificar el hint, el icono de la 'x' y el color del texto accediendo a los recursos
        que ofrece la api. También he visto que podia acceder a ellos desde el SearchView, pero es lo mismo:

            EditText etSearch = svBuscar.findViewById(androidx.appcompat.R.id.search_src_text);

        A otros recursos como el icon de la lupa o la linea sobre la que se escribe no he podido, a si que se
        ven un poco mal.
        He probado:
                ImageView icon = findViewById(androidx.appcompat.R.id.search_mag_icon);
                icon.setColorFilter(getResources().getColor(R.color.primary_toolbar, getTheme()));

         y cambiando el recurso por los otros 'search_*_*'
         */

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            EditText etSearch = findViewById(androidx.appcompat.R.id.search_src_text);
            etSearch.setHintTextColor(getResources().getColor(R.color.white, getTheme()));
            etSearch.setTextColor(getResources().getColor(R.color.white, getTheme()));
            ImageView iconClose = findViewById(androidx.appcompat.R.id.search_close_btn);
            iconClose.setColorFilter(getResources().getColor(R.color.white, getTheme()));
        } else {
            EditText etSearch = findViewById(androidx.appcompat.R.id.search_src_text);
            etSearch.setHintTextColor(getResources().getColor(R.color.primary_toolbar, getTheme()));
            etSearch.setTextColor(getResources().getColor(R.color.primary_toolbar, getTheme()));
            ImageView iconClose = findViewById(androidx.appcompat.R.id.search_close_btn);
            iconClose.setColorFilter(getResources().getColor(R.color.primary_toolbar, getTheme()));
        }
    }

    /**
     * Método para ocultar el teclado.
     */
    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Cuando hay un cambio de layout guardo la ciudad que se ha seleccionado
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedCity != null) {
            outState.putSerializable("city", selectedCity);
        }
        AlertDialogCustom.destruirDialog();
    }

    /**
     * Restaro la ciudad seleccionada tras un cambio de layout
     *
     * @param savedInstanceState the data most recently supplied in {@link #onSaveInstanceState}.
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (!savedInstanceState.isEmpty() && savedInstanceState.getSerializable("city") != null) {
            selectedCity = (Ciudad) savedInstanceState.getSerializable("city");
            modificarInfo(selectedCity);
            btnMostrarMapa.setEnabled(true);
        }
    }

    /**
     * Este método comparo el texto que recibe como argumento con la lista de nombres de las ciudades.
     * En caso de coincidencia, se agrega la ciudad al cursor para que el sv muestre la lista de sugerencias
     *
     * @param text
     */
    private void filtrarCiudades(String text) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "name"});
        for (String s : ciudades.keySet()) {
            if (s.toLowerCase().contains(text.toLowerCase())) {
                cursor.addRow(new String[]{String.valueOf(ciudades.get(s).getId()), ciudades.get(s).getName()});
            }
        }
        adapterSearch.swapCursor(cursor);
        svBuscar.setSuggestionsAdapter(adapterSearch);
    }

    /**
     * Este método modifica la información que se muestra sobre la ciudad seleccionada.
     *
     * @param c
     */
    private void modificarInfo(Ciudad c) {
        tvNombre.setText(c.getName());
        tvLatitud.setText(String.valueOf(c.getCoord().getLat()));
        tvLongitud.setText(String.valueOf(c.getCoord().getLon()));
        tvPais.setText(c.getCountry());
    }

    /**
     * Con este método actualizo y muestro el dialog personalizado
     */
    private static void mostrarDialog() {
        AlertDialogCustom.modificarEstado(estado);
        AlertDialogCustom.mostrarDialog(context);
    }

    /**
     * Método para controlar los Toast
     *
     * @param mensaje  a mostrar
     * @param duration Toast.LENGTH_LONG - Toast.LENGTH_SHORT
     */
    public static void mostrarToast(String mensaje, int duration) {
        if (toast != null && toast.getDuration() > 0) toast.cancel();
        toast = Toast.makeText(context, mensaje, duration);
        toast.show();
    }

    public static String getIpServer() {
        return ipServer;
    }

    public static void setIpServer(String ipServer) {
        MainActivity.ipServer = ipServer;
    }

    public static String getPath() {
        return path;
    }

    public static void setPath(String path) {
        MainActivity.path = path;
    }

}