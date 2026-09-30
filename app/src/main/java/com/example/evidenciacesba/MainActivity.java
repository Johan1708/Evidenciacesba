package com.example.evidenciacesba;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // COMPONENTES DE LA INTERFAZ
    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;

    private Button btnTomarFoto;
    private Button btnGuardar;
    private Button btnCompartir;

    private TextView txtEstado;
    private TextView txtFechaHora;
    private Spinner spnEstado;

    // VARIABLES DE DATOS
    private Bitmap fotografiaBitmap = null;
    private String fechaCaptura = "";
    private String horaCaptura = "";

    /*
     * Launcher para recibir el resultado de la cámara
     */
    private final ActivityResultLauncher<Intent> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() == RESULT_OK) {
                            Intent datos = resultado.getData();

                            if (datos != null && datos.getExtras() != null) {
                                fotografiaBitmap = (Bitmap) datos.getExtras().get("data");
                                imgFoto.setImageBitmap(fotografiaBitmap);

                                txtEstado.setText("Fotografía tomada correctamente");

                                // Capturar Fecha y Hora
                                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                fechaCaptura = dateFormat.format(new Date());
                                horaCaptura = timeFormat.format(new Date());

                                txtFechaHora.setText("Capturado el: " + fechaCaptura + " a las " + horaCaptura);
                                txtFechaHora.setVisibility(View.VISIBLE);

                                Toast.makeText(MainActivity.this, "Fotografía recibida", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            txtEstado.setText("La fotografía fue cancelada");
                            Toast.makeText(MainActivity.this, "Operación cancelada", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ENLACE CON XML
        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCompartir = findViewById(R.id.btnCompartir);
        txtEstado = findViewById(R.id.txtEstado);
        txtFechaHora = findViewById(R.id.txtFechaHora);
        spnEstado = findViewById(R.id.spnEstado);

        // CONFIGURACIÓN CORRECTA DEL SPINNER
        String[] opcionesEstado = {"Pendiente", "En Proceso", "Terminada"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesEstado);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnEstado.setAdapter(adapter);

        // LISTENERS
        btnTomarFoto.setOnClickListener(v -> abrirCamara());
        btnGuardar.setOnClickListener(v -> guardarEvidencia());
        btnCompartir.setOnClickListener(v -> compartirEvidencia());
    }

    /*
     * MÉTODO ABRIR CÁMARA CORREGIDO
     */
    private void abrirCamara() {
        Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            launcherCamara.launch(intentCamara);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_LONG).show();
        }
    }

    private void guardarEvidencia() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        if (spnEstado.getSelectedItem() == null) return;
        String estadoSeleccionado = spnEstado.getSelectedItem().toString();

        if (titulo.isEmpty()) {
            edtTitulo.setError("Escribe un título");
            edtTitulo.requestFocus();
            return;
        }

        if (descripcion.isEmpty()) {
            edtDescripcion.setError("Escribe una descripción");
            edtDescripcion.requestFocus();
            return;
        }

        if (fotografiaBitmap == null) {
            Toast.makeText(this, "Primero debes tomar una fotografía", Toast.LENGTH_LONG).show();
            return;
        }

        guardarFotoEnGaleria(titulo);

        txtEstado.setText("Evidencia registrada correctamente en estado: " + estadoSeleccionado);
        Toast.makeText(this, "Evidencia guardada en la Galería", Toast.LENGTH_LONG).show();

        btnCompartir.setVisibility(View.VISIBLE);
    }

    private void guardarFotoEnGaleria(String nombre) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, nombre + "_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EvidenciasCESBA");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (fotografiaBitmap != null) {
                    fotografiaBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void compartirEvidencia() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        String estadoSeleccionado = spnEstado.getSelectedItem().toString();

        String mensajeCompartir = "📌 Evidencia CESBA\n" +
                "• Título: " + titulo + "\n" +
                "• Estado: " + estadoSeleccionado + "\n" +
                "• Fecha: " + fechaCaptura + " " + horaCaptura + "\n" +
                "• Descripción: " + descripcion;

        Intent intentCompartir = new Intent(Intent.ACTION_SEND);
        intentCompartir.setType("text/plain");
        intentCompartir.putExtra(Intent.EXTRA_TEXT, mensajeCompartir);
        startActivity(Intent.createChooser(intentCompartir, "Compartir evidencia vía:"));
    }
}