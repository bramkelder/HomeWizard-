package idu.stenden.inf1i.homewizard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class AddHomewizardlight extends BaseMqttEventActivity {

    private MqttController mqttController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_homewizardlight);

        final EditText HMWZlightname = (EditText) findViewById(R.id.HMWZlightname);
        final EditText HMWZlightcode = (EditText) findViewById(R.id.HMWZlightcode);

        mqttController = MqttController.getInstance();
        mqttController.setContext(getApplicationContext());

        Button generateKaku = (Button) findViewById(R.id.HMWZlightgenerate);

        generateKaku.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MqttController controller = MqttController.getInstance();
                controller.publish("HYDRA/HMWZ/sw", "generatekaku");
            }
        });

        Button addHMWZ = (Button) findViewById(R.id.HMWZlightadd);

        addHMWZ.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!HMWZlightname.getText().toString().isEmpty() && !HMWZlightcode.getText().toString().isEmpty()) {
                    mqttController.publish("HYDRA/HMWZ/sw/add/" + HMWZlightname.getText() + "/switch/" + HMWZlightcode.getText(), "/0");

                    finish();
                    //This just adds to the activity stack, we don't want that!
                    //startActivity(new Intent(getApplicationContext(), MainActivity.class));
                } else {
                    Toast toaster = Toast.makeText(getApplicationContext(), "Vul velden in", Toast.LENGTH_SHORT);
                    toaster.show();
                }
            }
        });
    }

    @Override
    protected void addEventListeners() {
        addEventListener(new MqttControllerMessageCallbackListener() {
            @Override
            public void onMessageArrived(String topic, MqttMessage message) {
                if(topic.equals("HYDRA/HMWZRETURN/sw")){
                    try {
                        JSONObject jsonObject = new JSONObject(message.toString());
                        String route = jsonObject.getJSONObject("request").getString("route");
                        if(route.equals("/sw/generatekaku")){
                            String code = jsonObject.getJSONObject("response").getString("code");
                            EditText HMWZlightcode = (EditText) findViewById(R.id.HMWZlightcode);
                            HMWZlightcode.setText(code);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }
}