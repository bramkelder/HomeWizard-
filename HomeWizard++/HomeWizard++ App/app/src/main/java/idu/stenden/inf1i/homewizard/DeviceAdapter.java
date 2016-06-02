package idu.stenden.inf1i.homewizard;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import idu.stenden.inf1i.homewizard.ColorPickerDialog.OnColorChangedListener;

/**
 * Created by Bram on 19/05/2016.
 */


class DeviceAdapter extends ArrayAdapter<BaseSwitch> {

    private static final int VIEWTYPE_SWITCH    = 0;
    private static final int VIEWTYPE_DIMMER    = 1;
    private static final int VIEWTYPE_HUE       = 2;
    private static final int VIEWTYPE_COUNT = VIEWTYPE_HUE + 1;
    final Context context = getContext();

    public DeviceAdapter(Context context, int resource) {
        super(context, resource);
    }

    public DeviceAdapter(Context context, int resource, int textViewResourceId, List<?> objects) {
        super(context, resource, textViewResourceId, (List<BaseSwitch>) objects);
    }

    @Override
    public int getItemViewType(int position){
        Log.e("DeviceAdapter", "getItemViewType, " + position);
        BaseSwitch sw = getItem(position);
        if(sw.getType().equals("dimmer")){
            return VIEWTYPE_DIMMER;
        }if(sw.getType().equals("hue")){
            return VIEWTYPE_HUE;
        } else {
            return VIEWTYPE_SWITCH;
        }
    }

    @Override
    public int getViewTypeCount(){
        return VIEWTYPE_COUNT;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final BaseSwitch sw = getItem(position);
        int viewType = getItemViewType(position);


        if(convertView == null) {
            switch(viewType) {
                case VIEWTYPE_DIMMER:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_dim, parent, false);
                    break;
                case VIEWTYPE_SWITCH:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);
                    break;
                case VIEWTYPE_HUE:
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_hue, parent, false);
                    break;
            }
        }

        //Sub-views
        final TextView swName;
        final Switch swSwitch;
        final SeekBar swBar;

        // Stop the switch from sending an update when we set the UI correctly
        sw.setRespondToInput(false);

        switch(viewType) {
            case VIEWTYPE_DIMMER: {
                //Treat it as a dimmer
                swName = (TextView) convertView.findViewById(R.id.rowDimTextView);
                swBar = (SeekBar) convertView.findViewById(R.id.rowDimSeekBar);

                swBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        //Not relevant
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        //Not relevant
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if(!sw.respondToInput()) {
                            return;
                        }
                        if(HomewizardSwitch.class.isInstance(sw)) {
                            HomewizardSwitch homewizardSwitch = (HomewizardSwitch)sw;
                            //User stopped touching, set correct dim level
                            if(!homewizardSwitch.isUpdating()) {
                                int dimValue = seekBar.getProgress();

                                seekBar.setEnabled(false);

                                homewizardSwitch.setDimmer(dimValue);
                                homewizardSwitch.setStatus(dimValue > 0);
                                homewizardSwitch.sendStatus();
                                homewizardSwitch.sendDimmer();
                                homewizardSwitch.setUpdating(true);
                            }
                        } else {
                            //CustomSwitch
                            int dimValue = seekBar.getProgress();

                            sw.setDimmer(dimValue);
                            sw.sendDimmer();
                        }

                    }
                });

                if(HomewizardSwitch.class.isInstance(sw)) {
                    HomewizardSwitch homewizardSwitch = (HomewizardSwitch)sw;
                    swBar.setMax(100);
                    swBar.setProgress(homewizardSwitch.getDimmer());

                    swBar.setEnabled(!homewizardSwitch.isUpdating());
                } else {
                    //CustomSwitch
                    swBar.setMax(100);
                    swBar.setProgress(sw.getDimmer());
                    swBar.setEnabled(true);
                }

            } break;
            case VIEWTYPE_SWITCH: {
                //Treat it as a switch
                swName = (TextView) convertView.findViewById(R.id.rowTextView);
                swSwitch = (Switch) convertView.findViewById(R.id.rowSwitch);

                swSwitch.setOnCheckedChangeListener((new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(!sw.respondToInput()) {
                            return;
                        }
                        if(HomewizardSwitch.class.isInstance(sw)) {
                            HomewizardSwitch homewizardSwitch = (HomewizardSwitch) sw;
                            //If we're not still waiting for a response from a previous toggle
                            if (!homewizardSwitch.isUpdating()) {
                                buttonView.setEnabled(false);

                                homewizardSwitch.setStatus(isChecked);
                                homewizardSwitch.sendStatus();
                                homewizardSwitch.setUpdating(true);
                            }
                        } else {
                            //CustomSwitch
                            sw.setStatus(isChecked);
                            sw.sendStatus();
                        }
                    }
                }));

                if(HomewizardSwitch.class.isInstance(sw)) {
                    HomewizardSwitch homewizardSwitch = (HomewizardSwitch) sw;
                    swSwitch.setChecked(sw.getStatus());
                    swSwitch.setEnabled(!homewizardSwitch.isUpdating());
                } else {
                    //CustomSwitch
                    swSwitch.setChecked(sw.getStatus());
                    swSwitch.setEnabled(true);
                }
            } break;
            case VIEWTYPE_HUE: {
                //Treat it as a HUE
                //TODO: Add color picker and on/off button
                swName = (TextView) convertView.findViewById(R.id.rowHueTextView);
                swSwitch = (Switch) convertView.findViewById(R.id.rowHueSwitch);
                final SeekBar swSeekbar = (SeekBar) convertView.findViewById(R.id.seekBarHue);

                final Button swButton = (Button) convertView.findViewById(R.id.rowHueButton);

                final int lightID = 1;

                swSwitch.setOnCheckedChangeListener((new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(!sw.respondToInput()) {
                            return;
                        }

                        if(!isChecked){
                            JSONObject payload = new JSONObject();
                            try {
                                payload.put("lights", lightID);

                                JSONObject command = new JSONObject();
                                command.put("on", false);
                                payload.put("command", command);

                                MqttController.getInstance().publish("HYDRA/HUE/set-light", payload.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            JSONObject payload = new JSONObject();
                            try {
                                payload.put("lights", lightID);

                                JSONObject command = new JSONObject();
                                command.put("on", true);
                                payload.put("command", command);

                                MqttController.getInstance().publish("HYDRA/HUE/set-light", payload.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }));

                swButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        new ColorPickerDialog(context, new OnColorChangedListener() {
                            @Override
                            public void colorChanged(int color) {
                                swButton.setBackgroundColor(color);

                                int RGB = (int)Long.parseLong("" + color, 16);
                                int r = (color >> 16) & 0xFF;
                                int g = (color >> 8) & 0xFF;
                                int b = (color >> 0) & 0xFF;

                                float[] hsv = new float[3];
                                Color.RGBToHSV(r, g, b, hsv);

                                JSONObject payload = new JSONObject();
                                try {
                                    //payload.put("lights", lightID);

                                    JSONObject command = new JSONObject();
                                    command.put("on", true);
                                    /*//command.put("hue", Math.ceil((hsv[0] / 360) * 65536));
                                    //command.put("sat", Math.ceil(hsv[1] * 255));

                                    double[] xy = {xyB[0], xyB[1]};
                                    command.put("xy", new JSONArray(Arrays.asList(xy)));
                                    //command.put("bri", xyB[2]);*/

                                    //payload.put("command", command);
                                    float[] xyB = Util.RGBtoXYB(color);
                                    String pld = "{\"lights\":1, \"command\":{\"on\": true, \"xy\": [" + String.valueOf(xyB[0]) + "," + String.valueOf(xyB[1]) + "]}, \"bri\": " + String.valueOf((int)Math.ceil(xyB[2] * 255f)) + "}";

                                    MqttController.getInstance().publish("HYDRA/HUE/set-light", pld);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }, 0xFFFF0000).show();
                    }
                });

                swSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("lights", lightID);

                            JSONObject command = new JSONObject();
                            command.put("on", true);
                            command.put("bri", Math.ceil(swSeekbar.getProgress()));

                            payload.put("command", command);

                            MqttController.getInstance().publish("HYDRA/HUE/set-light", payload.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            } break;
            default: {
                // Shouldn't ever happen but stops the compiler from complaining
                swName = null;
            }
        } //switch

        swName.setText(sw.getName());

        //Re-enable the switch
        sw.setRespondToInput(true);

        return convertView;
    }
	
	@Override
	public void clear() {
        Log.i("DeviceAdapter", "Clearing DeviceAdapter");
		super.clear();
		//MqttController.getInstance().removeMessageListeners((MqttControllerMessageCallbackListener[])viewMessageCallbacks.toArray());
	}

}
