package idu.stenden.inf1i.homewizard;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Bram on 23/05/2016.
 */
public class AppDataContainer implements MqttControllerMessageCallbackListener
{

    static AppDataContainer instance;

    private ArrayList<HomewizardSwitch> homewizardSwitches = new ArrayList<HomewizardSwitch>();
    private ArrayList<CustomSwitch> customSwitches = new ArrayList<>();
    private ArrayList<HueSwitch> hueSwitches = new ArrayList<>();

    private ArrayList<BaseSwitch> allSwitches = new ArrayList<>();

    private DeviceAdapter deviceAdapter;
    private DeviceEditAdapter deviceEditAdapter;
	
	private Context saveContext;

    private AppDataContainer()
    {
        registerEventHandler();
    }

    public static AppDataContainer getInstance()
    {
        if(instance == null)
        {
            instance = new AppDataContainer();
        }
        return instance;
    }
	
	public void setSaveContext(Context context)
    {
		saveContext = context;
	}

    //TODO: Save/load of non-homewizard data?
    public void save()
    {
		Util.saveCustomSwitch(saveContext, customSwitches);
    }

    public void load()
    {
		try
        {
			customSwitches.clear();
			customSwitches.addAll(Util.readCustomSwitch(saveContext));
            updateAllSwitches();
		}
        catch (Exception e1)
        {
			Log.e("AppDataContainer", e1.toString());
		}
    }


    public void notifyDataSetChanged()
    {
        deviceAdapter.notifyDataSetChanged();
        deviceEditAdapter.notifyDataSetChanged();
    }




    public ArrayList<CustomSwitch> getCustomSwitches()
    {
        return customSwitches;
    }

    public void addCustomSwitch(CustomSwitch customSwitch)
    {
        customSwitches.add(customSwitch);
        updateAllSwitches();
    }

    public CustomSwitch findCustomSwitchByName(String name)
    {
        for(CustomSwitch sw : customSwitches)
        {
            if(sw.name.equals(name))
            {
                return sw;
            }
        }
        return null;
    }

    public void removeCustomSwitch(CustomSwitch customSwitch)
    {
        customSwitches.remove(customSwitch);
        updateAllSwitches();
    }

    public void clearCustomSwitches()
    {
        customSwitches.clear();
        updateAllSwitches();
    }

    public ArrayList<HueSwitch> getHueSwitches()
    {
        return hueSwitches;
    }

    public void addHueSwitch(HueSwitch hueSwitch)
    {
        hueSwitches.add(hueSwitch);
        updateAllSwitches();
    }

    public void removeCustomSwitch(HueSwitch hueSwitch)
    {
        hueSwitches.remove(hueSwitch);
        updateAllSwitches();
    }

    public void clearHueSwitches()
    {
        hueSwitches.clear();
        updateAllSwitches();
    }

    public ArrayList<HomewizardSwitch> getHomewizardSwitches()
    {
        return homewizardSwitches;
    }

    public void addHomewizardSwitch(HomewizardSwitch homewizardSwitch)
    {
        homewizardSwitches.add(homewizardSwitch);
        updateAllSwitches();
    }

    public void clearHomewizardSwitches()
    {
        homewizardSwitches.clear();
        updateAllSwitches();
    }

    public ArrayList<BaseSwitch> getAllSwitches()
    {
        return allSwitches;
    }

    private void updateAllSwitches()
    {
        allSwitches.clear();
        if(homewizardSwitches.size()> 0)
        {
            allSwitches.add(new CustomSwitch("HomeWizard", "separator"));
        }
        allSwitches.addAll(homewizardSwitches);

        if(customSwitches.size() > 0)
        {
            allSwitches.add(new CustomSwitch("Custom", "separator"));
        }
        allSwitches.addAll(customSwitches);

        if(hueSwitches.size() > 0)
        {
            allSwitches.add(new CustomSwitch("Philips Hue", "separator"));
        }
        allSwitches.addAll(hueSwitches);
    }

    public DeviceAdapter getDeviceAdapter()
    {
        return deviceAdapter;
    }

    public void setDeviceAdapter(DeviceAdapter deviceAdapter)
    {
        this.deviceAdapter = deviceAdapter;
    }

    public DeviceEditAdapter getDeviceEditAdapter()
    {
        return deviceEditAdapter;
    }

    public void setDeviceEditAdapter(DeviceEditAdapter deviceEditAdapter)
    {
        this.deviceEditAdapter = deviceEditAdapter;
    }

    private void registerEventHandler()
    {
        MqttController.getInstance().addMessageListener(this);
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message)
    {
        if(topic.contains("HYDRA/HUERETURN/get-lights"))
        {
            try
            {
                hueSwitches.clear();

                JSONArray jsonObject = new JSONArray(message.toString());
                for(int i = 0; i < jsonObject.length(); i++)
                {
                    JSONObject obj = jsonObject.getJSONObject(i);
                    HueSwitch hueSwitch = new HueSwitch();
                    hueSwitch.setHueType(obj.getString("type"));
                    hueSwitch.setName(obj.getString("name"));
                    hueSwitch.setBrightness(obj.getInt("brightness"));
                    hueSwitch.setStatus(obj.getBoolean("on"));
                    hueSwitch.setId(obj.getInt("id"));

                    if(hueSwitch.isColorLight())
                    {

                        hueSwitch.setHue(obj.getInt("hue"));
                        hueSwitch.setSaturation(obj.getInt("saturation"));
                        JSONArray xy = obj.getJSONArray("xy");
                        float x = (float) xy.getDouble(0);
                        float y = (float) xy.getDouble(1);
                        Log.d("AppDataContainer", "x: " + x + " y: " + y);
                        hueSwitch.setXy(new float[]{x, y});

                        hueSwitch.setColormode(obj.getString("colormode"));
                    }
                    hueSwitches.add(hueSwitch);
                }

                updateAllSwitches();
                notifyDataSetChanged();
            }
            catch (JSONException e)
            {
                Log.e("AppDataContainer", e.getMessage());
            }

        }
        else if(topic.contains("HYDRA/HMWZRETURN/sw"))
        {
            //NOTE: try/catch is not required to stop crashing because MqttController handles this
            try
            {
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
                if(topic.contains("/dim/"))
                {
                    //Loop de loop
                    for (HomewizardSwitch sw : homewizardSwitches)
                    {
                        if(sw.getId() == id)
                        {
                            if(sw.isUpdating())
                            {
                                if(jsonObject.getString("status").equals("ok"))
                                {
                                    //Success
                                }
                                else
                                {
                                    //Resetting to 0 to indicate something went wrong
                                    sw.setDimmer(0);
                                }
                                sw.setUpdating(false);
                                //Notify adapter to update ui
                                deviceAdapter.notifyDataSetChanged();
                                deviceEditAdapter.notifyDataSetChanged();
                            }
                            else
                            {
                                Log.e("AppDataContainer", "Got message for non-updating switch! Id =" + id);
                            }
                            break;
                        }
                    }
                }
                else if(route.equals("/sw/add"))
                {
                    //Switch was added
                }
                else if(route.equals("/sw/remove"))
                {
                    //Switch was removed
                }
                else
                {
                    //Try normal switch on/off
                    for (HomewizardSwitch sw : homewizardSwitches)
                    {
                        if(sw.getId() == id)
                        {
                            if(sw.getType().equals("dimmer"))
                            {
                                continue;
                            }
                            if(sw.isUpdating())
                            {
                                if(jsonObject.getString("status").equals("ok"))
                                {
                                    //Success
                                }
                                else
                                {
                                    //Flip switch data back to old status
                                    sw.setStatus(!sw.getStatus());
                                }
                                sw.setUpdating(false);
                                //Notify adapter to update ui
                                deviceAdapter.notifyDataSetChanged();
                                deviceEditAdapter.notifyDataSetChanged();
                            }
                            else
                            {
                                Log.e("AppDataContainer", "Got message for non-updating switch! Id =" + id);
                            }
                            break;
                        }
                    }
                }
            }
            catch (JSONException e)
            {
                Log.e("AppDataContainer", e.toString());
            }
        }
    }
}
