package com.example.facedetecttion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

public class EmocionReceiver extends BroadcastReceiver {

    private TextView tvEmocion;
    private FrameLayout emojiContainer;

    private List<String> emojiSonrientes;
    private List<String> emojiSerios;
    private List<String> emojiNeutrales;

    // Constructor recibe referencias de UI y listas de emojis
    public EmocionReceiver(TextView tvEmocion, FrameLayout emojiContainer,
                           List<String> emojiSonrientes, List<String> emojiSerios, List<String> emojiNeutrales) {
        this.tvEmocion = tvEmocion;
        this.emojiContainer = emojiContainer;
        this.emojiSonrientes = emojiSonrientes;
        this.emojiSerios = emojiSerios;
        this.emojiNeutrales = emojiNeutrales;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String emocion = intent.getStringExtra("emocion");
        if (emocion != null) {
            // Actualiza el TextView
            tvEmocion.setText(emocion);

            // Muestra emojis según emoción
            if (emocion.contains("Sonriente")) {
                mostrarEmojis(emojiSonrientes);
            } else if (emocion.contains("Serio")) {
                mostrarEmojis(emojiSerios);
            } else {
                mostrarEmojis(emojiNeutrales);
            }
        }
    }

    private void mostrarEmojis(List<String> emojis) {
        emojiContainer.removeAllViews();

        for (String emoji : emojis) {
            TextView tv = new TextView(tvEmocion.getContext());
            tv.setText(emoji);
            tv.setTextSize(32f);

            // Posición aleatoria dentro del FrameLayout
            int width = emojiContainer.getWidth() - 100;
            int height = emojiContainer.getHeight() - 100;

            if (width < 0) width = 1;
            if (height < 0) height = 1;

            int x = new Random().nextInt(width);
            int y = new Random().nextInt(height);
            tv.setX(x);
            tv.setY(y);

            emojiContainer.addView(tv);

            // Desaparece después de 1 segundo
            tv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    emojiContainer.removeView(tv);
                }
            }, 1000);

        }
    }
}