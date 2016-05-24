package idu.stenden.inf1i.homewizard;

import android.content.Context;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bram on 19/05/2016.
 */

interface MqttControllerMessageCallbackListener{
    void onMessageArrived(String topic, MqttMessage message);
}

public class MqttController {

    static MqttController instance;

    private Context context;
    private MqttAndroidClient client;
    private boolean connectSucces = false;

    private List<MqttControllerMessageCallbackListener> messageListeners = new ArrayList<MqttControllerMessageCallbackListener>();

    private MqttController(){

    }

    public static MqttController getInstance(){
        if(instance == null){
            instance = new MqttController();
        }
        return instance;
    }

    public void addMessageListener(MqttControllerMessageCallbackListener listener)
    {
        messageListeners.add(listener);
    }

    public void setContext(Context applicationContext){
        context = applicationContext;
    }

    public boolean isConnected()
    {
        if(client != null){
            return client.isConnected();
        }else{
            return false;
        }
    }

    public void connect(String broker, String clientId){
        client =  new MqttAndroidClient(context, broker, clientId);

        try {
            client.connect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    Toast toast = Toast.makeText(context, "Connected to broker", Toast.LENGTH_SHORT);
                    toast.show();

                    client.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {

                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {

                            for(MqttControllerMessageCallbackListener listener : messageListeners){
                                listener.onMessageArrived(topic, message);
                            }
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {

                        }
                    });

                    connectSucces = true;
                    subscribe("HYDRA/HMWZRETURN");
                    subscribe("HYDRA/HMWZRETURN/#");
                    subscribe("HYDRA/STATUS/results");
                    publish("HYDRA/STATUS", "get-status");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    connectSucces = false;
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload){

        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(2);
        message.setRetained(false);

        if(connectSucces){
            try {
                client.publish(topic, message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else{
            Toast toast = Toast.makeText(context, "Could not connect to broker", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void subscribe(String topic){
        try {
            client.subscribe(topic, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
