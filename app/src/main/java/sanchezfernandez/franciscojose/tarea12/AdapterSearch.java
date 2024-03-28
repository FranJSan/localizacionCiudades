package sanchezfernandez.franciscojose.tarea12;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cursoradapter.widget.CursorAdapter;

import android.widget.ImageView;
import android.widget.TextView;

/**
 * Esta clase hace de adpatador para el SearchView y sus sugerencias
 * Mediante un cursor
 */
public class AdapterSearch extends CursorAdapter {

    private Context context;
    private Cursor cursor;

    /**
     * El constructor recibe el context y un cursor que contiene la lista de ciudades
     * @param context
     * @param cursor
     */
    public AdapterSearch(Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.context = context;
        this.cursor = cursor;
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
    }

    /**
     * En este método obtendo el nombre de la ciudad contido en el cursos y se lo agrego al TextView
     * que se mostrará con cada sugerencia
     * @param view Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param cursor The cursor from which to get the data. The cursor is already
     * moved to the correct position.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String nombreCiudad = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText(nombreCiudad);

    }
}
