package idu.stenden.inf1i.homewizard;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class AddHomewizardlight extends BaseMqttEventActivity
{

    private MqttController mqttController;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_homewizardlight);

        final EditText HMWZlightname = (EditText) findViewById(R.id.HMWZlightname);
        final EditText HMWZlightcode = (EditText) findViewById(R.id.HMWZlightcode);
        final CheckBox HMWZlightdimmer = (CheckBox) findViewById(R.id.HMWZlightdimmer);

        mqttController = MqttController.getInstance();
        mqttController.setContext(getApplicationContext());

        Button generateKaku = (Button) findViewById(R.id.HMWZlightgenerate);

        generateKaku.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                MqttController controller = MqttController.getInstance();
                controller.publish("HYDRA/HMWZ/sw", "generatekaku");
            }
        });

        Button learnKaku = (Button) findViewById(R.id.HMWZlightaddlearn);

        learnKaku.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                MqttController controller = MqttController.getInstance();
                controller.publish("HYDRA/HMWZ/sw", "learn");
            }
        });

        Button addHMWZ = (Button) findViewById(R.id.HMWZlightadd);

        addHMWZ.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if (!HMWZlightname.getText().toString().isEmpty() && !HMWZlightcode.getText().toString().isEmpty())
                {
                    String name = HMWZlightname.getText().toString().trim().replace(" ", "+");
                    String code = HMWZlightcode.getText().toString().trim().replace(" ", "+");

                    if (HMWZlightdimmer.isChecked())
                    {
                        mqttController.publish("HYDRA/HMWZ/sw/add", name + "/dimmer/" + code + "/0");
                    }
                    else
                    {
                        mqttController.publish("HYDRA/HMWZ/sw/add", name + "/switch/" + code + "/0");

                    }

                    finish();
                }
                else
                {
                    Toast toaster = Toast.makeText(getApplicationContext(), "Fields cannot be empty", Toast.LENGTH_SHORT);
                    toaster.show();
                }
            }
        });
    }

    @Override
    protected void addEventListeners()
    {
        addEventListener(new MqttControllerMessageCallbackListener()
        {
            @Override
            public void onMessageArrived(String topic, MqttMessage message)
            {
                if (topic.equals("HYDRA/HMWZRETURN/sw"))
                {
                    try
                    {
                        JSONObject jsonObject = new JSONObject(message.toString());
                        String route = jsonObject.getJSONObject("request").getString("route");

                        if (route.equals("/sw/generatekaku"))
                        {
                            try
                            {

                                String code = jsonObject.getJSONObject("response").getString("code");
                                EditText HMWZlightcode = (EditText) findViewById(R.id.HMWZlightcode);
                                HMWZlightcode.setText(code);
                            }
                            catch (Exception e)
                            {
                                Log.e("AddHomewizardlight", e.getMessage());
                            }
                        }

                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
