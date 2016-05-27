package idu.stenden.inf1i.homewizard;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bram on 23/05/2016.
 */
public class AppDataContainer implements MqttControllerMessageCallbackListener {

    static AppDataContainer instance;

    private ArrayList<HomewizardSwitch> homewizardSwitches = new ArrayList<HomewizardSwitch>();
    private ArrayList<CustomSwitch> customSwitches = new ArrayList<>();

    private DeviceAdapter deviceAdapter;
    private DeviceEditAdapter deviceEditAdapter;


    private AppDataContainer(){
        registerEventHandler();
    }

    public static AppDataContainer getInstance(){
        if(instance == null){
            instance = new AppDataContainer();
        }
        return instance;
    }


    //TODO: Save/load of non-homewizard data?
    public void save() {

    }

    public void load() {

    }

    public ArrayList<CustomSwitch> getCustomSwitches() {
        return customSwitches;
    }

    public void addCustomSwitch(CustomSwitch customSwitch) {
        customSwitches.add(customSwitch);
    }

    public void clearCustomSwitches() {
        customSwitches.clear();
    }

    public ArrayList<HomewizardSwitch> getHomewizardSwitches(){
        return homewizardSwitches;
    }

    public void addHomewizardSwitch(HomewizardSwitch homewizardSwitch){
        homewizardSwitches.add(homewizardSwitch);
    }

    public void clearHomewizardSwitches(){
        homewizardSwitches.clear();
    }

    public DeviceAdapter getDeviceAdapter() {
        return deviceAdapter;
    }

    public void setDeviceAdapter(DeviceAdapter deviceAdapter) {
        this.deviceAdapter = deviceAdapter;
    }

    public DeviceEditAdapter getDeviceEditAdapter() {
        return deviceEditAdapter;
    }

    public void setDeviceEditAdapter(DeviceEditAdapter deviceEditAdapter) {
        this.deviceEditAdapter = deviceEditAdapter;
    }

    private void registerEventHandler() {
        MqttController.getInstance().addMessageListener(this);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) {
        if(topic.contains("HYDRA/HMWZRETURN/sw")) {
            //NOTE: try/catch is not required to stop crashing because MqttController handles this
            try {
                JSONObject jsonObject = new JSONObject(message.toString());
                String route = jsonObject.getJSONObject("request").getString("route");
                /*
                    Options:
                    /sw/ID      (on/off)
                    /sw/dim     (dim level)
                    /sw/add     (add...)
                    /sw/remove  (...)
                 */
                //Switch Id SHOULD be the last part of the topic
                int id = Integer.parseInt(topic.substring(topic.lastIndexOf("/")+1));
                if(route.equals("/sw/dim")) {
                    //Loop de loop
                    for (HomewizardSwitch sw : homewizardSwitches) {
                        if(sw.getId() == id) {
                            if(sw.isUpdating()) {
                                if(jsonObject.getString("status").equals("ok")) {
                                    //Success
                                } else {
                                }
                                sw.setUpdating(false);
                                //Notify adapter to update ui
                                deviceAdapter.notifyDataSetChanged();
                                deviceEditAdapter.notifyDataSetChanged();
                            } else {
                                Log.e("AppDataContainer", "Got message for non-updating switch! Id =" + id);
                            }
                            break;
                        }
                    }
                } else if(route.equals("/sw/add")) {
                    //Switch was added
                } else if(route.equals("/sw/remove")) {
                    //Switch was removed
                } else {
                    //Try normal switch on/off
                    for (HomewizardSwitch sw : homewizardSwitches) {
                        if(sw.getId() == id) {
                            if(sw.isUpdating()) {
                                if(jsonObject.getString("status").equals("ok")) {
                                    //Success
                                } else {
                                    //Flip switch data back to old status
                                    sw.setStatus(!sw.getStatus());
                                }
                                sw.setUpdating(false);
                                //Notify adapter to update ui
                                deviceAdapter.notifyDataSetChanged();
                                deviceEditAdapter.notifyDataSetChanged();
                            } else {
                                Log.e("AppDataContainer", "Got message for non-updating switch! Id =" + id);
                            }
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e("AppDataContainer", e.toString());
            }
        }
    }
}
