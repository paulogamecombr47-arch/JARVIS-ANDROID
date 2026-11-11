
package com.jarvis.android;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;
import android.graphics.Color;
import android.view.Gravity;
import android.content.Context;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MainActivity extends Activity implements RecognitionListener {

    private TextView resposta;
    private SpeechRecognizer reconhecedor;
    private TextToSpeech voz;
    private boolean vozPronta = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setGravity(Gravity.CENTER);
        tela.setPadding(30, 40, 30, 40);
        tela.setBackgroundColor(Color.rgb(5, 12, 25));

        TextView titulo = new TextView(this);
        titulo.setText("J.A.R.V.I.S");
        titulo.setTextSize(32);
        titulo.setTextColor(Color.CYAN);
        titulo.setGravity(Gravity.CENTER);

        resposta = new TextView(this);
        resposta.setText("Às suas ordens, Senhor.");
        resposta.setTextSize(19);
        resposta.setTextColor(Color.WHITE);
        resposta.setGravity(Gravity.CENTER);
        resposta.setPadding(10, 50, 10, 50);

        Button falar = new Button(this);
        falar.setText("FALAR COM JARVIS");

        Button testar = new Button(this);
        testar.setText("TESTAR VOZ");

        tela.addView(titulo);
        tela.addView(resposta);
        tela.addView(falar);
        tela.addView(testar);

        setContentView(tela);

        voz = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int resultado = voz.setLanguage(new Locale("pt", "BR"));
                vozPronta = resultado != TextToSpeech.LANG_MISSING_DATA
                        && resultado != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        falar.setOnClickListener(v -> iniciarReconhecimento());

        testar.setOnClickListener(v ->
                falarResposta("Sistema de voz funcionando, Senhor.")
        );

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, 100
            );
        }
    }

    private void iniciarReconhecimento() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            mostrar("Reconhecimento de voz indisponível neste aparelho.");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, 100
            );
            return;
        }

        if (reconhecedor != null) {
            reconhecedor.destroy();
        }

        reconhecedor = SpeechRecognizer.createSpeechRecognizer(this);
        reconhecedor.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR");

        mostrar("Estou ouvindo, Senhor...");
        reconhecedor.startListening(intent);
    }

    private void processarComando(String comando) {
        String c = Normalizer.normalize(
                comando.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "").trim();

        String resultado;

        if (c.contains("que dia e hoje")
                || c.contains("dia de hoje")
                || c.equals("data")
                || c.contains("qual a data")) {

            resultado = "Hoje é " +
                    new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy",
                            new Locale("pt", "BR")).format(new Date());

        } else if (c.contains("hora") || c.contains("horas")) {

            resultado = "Agora são " +
                    new SimpleDateFormat("HH:mm",
                            new Locale("pt", "BR")).format(new Date());

        } else if (c.contains("quem e voce")
                || c.contains("seu nome")) {

            resultado = "Sou JARVIS, seu assistente pessoal. Às suas ordens, Senhor.";

        } else if (c.contains("ola") || c.contains("oi")
                || c.contains("bom dia") || c.contains("boa tarde")
                || c.contains("boa noite")) {

            resultado = "Saudações, Senhor. Como posso ajudar?";

        } else if (c.contains("ajuda") || c.contains("comandos")) {

            resultado = "Posso informar a hora, a data, fazer contas e responder a algumas perguntas.";

        } else if (c.contains("obrigado") || c.contains("obrigada")) {

            resultado = "Sempre às ordens, Senhor.";

        } else if (c.contains("como voce esta")) {

            resultado = "Todos os sistemas estão funcionando, Senhor.";

        } else {
            resultado = calcular(c);

            if (resultado == null) {
                resultado = "Ainda não sei responder a isso, Senhor. Esse comando será adicionado em uma próxima atualização.";
            }
        }

        mostrar(resultado);
        falarResposta(resultado);
    }

    private String calcular(String comando) {
        Pattern padrao = Pattern.compile(
                "(-?\\d+(?:[,.]\\d+)?)\\s*(mais|menos|vezes|multiplicado por|dividido por|dividido|x|por|\\+|\\-|\\*|/)\\s*(-?\\d+(?:[,.]\\d+)?)"
        );

        Matcher m = padrao.matcher(comando);

        if (!m.find()) return null;

        try {
            double a = Double.parseDouble(m.group(1).replace(",", "."));
            double b = Double.parseDouble(m.group(3).replace(",", "."));
            String op = m.group(2);
            double r;

            if (op.equals("mais") || op.equals("+")) {
                r = a + b;
            } else if (op.equals("menos") || op.equals("-")) {
                r = a - b;
            } else if (op.equals("vezes") || op.equals("multiplicado por")
                    || op.equals("x") || op.equals("*")) {
                r = a * b;
            } else {
                if (b == 0) return "Não é possível dividir por zero, Senhor.";
                r = a / b;
            }

            if (Double.isNaN(r) || Double.isInfinite(r)) {
                return "Não consegui calcular esse resultado.";
            }

            String valor = String.format(Locale.US, "%.4f", r);
            valor = valor.replaceAll("0+$", "").replaceAll("\\.$", "");

            return "O resultado é " + valor.replace(".", ",") + ".";

        } catch (Exception e) {
            return "Não consegui realizar esse cálculo.";
        }
    }

    private void mostrar(String texto) {
        resposta.setText(texto);
    }

    private void falarResposta(String texto) {
        if (vozPronta && voz != null) {
            voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "jarvis");
        }
    }

    @Override
    public void onResults(Bundle resultados) {
        ArrayList<String> lista =
                resultados.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                );

        if (lista != null && !lista.isEmpty()) {
            processarComando(lista.get(0));
        } else {
            mostrar("Não consegui entender, Senhor.");
        }
    }

    @Override
    public void onError(int erro) {
        mostrar("Não consegui reconhecer. Tente falar novamente.");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        mostrar("Estou ouvindo, Senhor...");
    }

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float rmsdB) {}

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {
        mostrar("Processando...");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    @Override
    protected void onDestroy() {
        if (reconhecedor != null) {
            reconhecedor.destroy();
        }

        if (voz != null) {
            voz.stop();
            voz.shutdown();
        }

        super.onDestroy();
    }
}
