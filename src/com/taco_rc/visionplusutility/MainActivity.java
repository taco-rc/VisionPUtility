package com.taco_rc.visionplusutility;

import android.R.integer;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.util.*;

import android.widget.*;
import android.content.Context;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.jcraft.jsch.*;

import android.os.AsyncTask;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;

;

public class MainActivity extends Activity {

	JSch jsch;
	Session repeaterSession;
	Session cameraSession;

	SSHTask sshTask;

	private ArrayAdapter<Model> repeaterArrayAdapter;
	private ArrayAdapter<Model> cameraArrayAdapter;

	AlertDialog levelDialog;
	private Switch mySwitch;

	final CharSequence[] items = { "0dBm (1 mW)", "4dBm (2 mW)", "5dBm (3 mW)",
			"7dBm (5 mW)", "8dBm (6 mW)", "9dBm (7 mW)", "10dBm (10 mW)",
			"11dBm (12 mW)", "12dBm (15 mW)", "13dBm (19 mW)", "14dBm (25 mW)",
			"15dBm (31 mW)", "16dBm (39 mW)", "17dBm (50 mW)", "18dBm (63 mW)",
			"19dBm (79 mW)", "20dBm (100 mW)", };
	final int[] itemsInteger = { 0, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
			17, 18, 19, 20, };
	boolean[] itemsChecked = new boolean[items.length];
	int txPowerSelected[] = { 8, 3 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		ListView repeaterListView = (ListView) findViewById(R.id.repeater_list_view);
		repeaterListView.setSelector(R.drawable.listview_selector);

		repeaterArrayAdapter = new MyCustomArrayAdapter(this, getModel(),
				R.layout.list_row_normal_layout, R.id.row_text_view,
				R.id.row_text_value_view);
		repeaterListView.setAdapter(repeaterArrayAdapter);

		repeaterListView
				.setOnItemClickListener(new ListView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						showDialog_();
					}
				});

		ListView cameraListView = (ListView) findViewById(R.id.camera_list_view);
		cameraListView.setSelector(R.drawable.listview_selector);

		cameraArrayAdapter = new MyCustomArrayAdapter(this, getCameraModel(),
				R.layout.list_row_camera_tx, R.id.row_text_view1,
				R.id.row_text_value_view1);
		cameraListView.setAdapter(cameraArrayAdapter);

		cameraListView
				.setOnItemClickListener(new ListView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						showDialog__();
					}
				});

		mySwitch = (Switch) findViewById(R.id.check_on_off);

		// set the switch to ON
		mySwitch.setChecked(false);
		// attach a listener to check for changes in state
		mySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {

				if (isChecked) {
					mySwitch.setEnabled(false);
					// ssh to repeater
					new SSHTask(MainActivity.this).execute("192.168.1.1");
				} else {
					try {
						repeaterSession.disconnect();

					} catch (Exception e) {

					}
					try {
						cameraSession.disconnect();

					} catch (Exception e) {

					}
				}

			}
		});

		jsch = new JSch();
	}

	private List<Model> getModel() {
		List<Model> list = new ArrayList<Model>();
		list.add(get("Transmit Power", "12dBm (15 mW)"));

		return list;
	}

	private List<Model> getCameraModel() {
		List<Model> list = new ArrayList<Model>();
		list.add(get("Transmit Power", "7dBm (5 mW)"));

		return list;
	}

	private Model get(String s, String place) {
		return new Model(s, place);
	}

	public void showDialog_() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Transmit Power");
		builder.setSingleChoiceItems(items, txPowerSelected[0],
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						txPowerSelected[0] = item;
					}
				});
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				try {
					String command = "iw phy phy0 set txpower fixed "
							+ itemsInteger[txPowerSelected[0]] + "00";
					Channel channel = repeaterSession.openChannel("exec");
					((ChannelExec) channel).setCommand(command);
					channel.connect();
				} catch (Exception e) {
					levelDialog.dismiss();

					settingNotSetDialog("Transmit Power ");
					SetWifiSwitchChecked(false);
					
					// reset the power
					txPowerSelected[0] = 8;
					txPowerSelected[1] = 3;
				}
				
				TextView textValueView = (TextView) findViewById(R.id.row_text_value_view);
				textValueView.setText(items[txPowerSelected[0]]);
				TextView textValueView1 = (TextView) findViewById(R.id.row_text_value_view1);
				textValueView1.setText(items[txPowerSelected[1]]);
			}
		});

		levelDialog = builder.create();
		levelDialog.show();
	}

	public void showDialog__() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Transmit Power");
		builder.setSingleChoiceItems(items, txPowerSelected[1],
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						txPowerSelected[1] = item;
					}
				});
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				try {
					String command = "iw phy phy0 set txpower fixed "
							+ itemsInteger[txPowerSelected[1]] + "00";
					Channel channel = cameraSession.openChannel("exec");
					((ChannelExec) channel).setCommand(command);
					channel.connect();
				} catch (Exception e) {

					levelDialog.dismiss();

					settingNotSetDialog("Transmit Power ");
					SetWifiSwitchChecked(false);
					
					// reset the power
					txPowerSelected[0] = 8;
					txPowerSelected[1] = 3;
				}
				
				TextView textValueView = (TextView) findViewById(R.id.row_text_value_view);
				textValueView.setText(items[txPowerSelected[0]]);
				TextView textValueView1 = (TextView) findViewById(R.id.row_text_value_view1);
				textValueView1.setText(items[txPowerSelected[1]]);
			}
		});

		levelDialog = builder.create();
		levelDialog.show();
	}

	public void settingNotSetDialog(String settingString) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Alert");
		alertDialog.setMessage(settingString + "not set!");
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// here you can add functions
			}
		});

		alertDialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	class SSHTask extends AsyncTask<String, Integer, String> {

		public ProgressDialog progressDialog;
		public MainActivity mainActivity;

		public SSHTask(MainActivity a) {
			this.mainActivity = a;
		}

		@Override
		protected String doInBackground(String... arg0) {

			try {
				mainActivity.repeaterSession = this.mainActivity.jsch
						.getSession("root", "192.168.1.2", 22);
				mainActivity.repeaterSession.setPassword("19881209");

				Properties prop = new Properties();
				prop.put("StrictHostKeyChecking", "no");

				mainActivity.repeaterSession.setConfig(prop);

				mainActivity.repeaterSession.connect(20000);

			} catch (Exception e) {
				// String errstring = e.getMessage();
				mainActivity.repeaterSession = null;

				return "Connect to Vision+ Failed!";
			}

			try {
				mainActivity.cameraSession = this.mainActivity.jsch.getSession(
						"root", "192.168.1.1", 22);
				mainActivity.cameraSession.setPassword("19881209");

				Properties prop = new Properties();
				prop.put("StrictHostKeyChecking", "no");

				mainActivity.cameraSession.setConfig(prop);

				mainActivity.cameraSession.connect(20000);

			} catch (Exception e) {
				mainActivity.repeaterSession.disconnect();
				mainActivity.repeaterSession = null;

				mainActivity.cameraSession = null;

				return "Connect to Vision+ Failed!";
			}

			return "";
		}

		@Override
		protected void onPostExecute(String result) {
			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			if (result != "") {
				AlertDialog alertDialog = new AlertDialog.Builder(
						this.mainActivity).create();
				alertDialog.setTitle("Alert");
				alertDialog.setMessage(result);
				alertDialog.setButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// here you can add functions
							}
						});

				alertDialog.show();

				this.mainActivity.SetWifiSwitchChecked(false);
				
				return;
			}

			this.mainActivity.SetWifiSwitchChecked(true);

		}

		@Override
		protected void onPreExecute() {
			if (progressDialog == null) {
				progressDialog = new ProgressDialog(MainActivity.this);
				progressDialog.setMessage("Connecting to Vision+");
				progressDialog.show();
				progressDialog.setCanceledOnTouchOutside(false);
				progressDialog.setCancelable(false);
			}
		}

		protected void onProgressUpdate(Integer... progress) {

		}

	}

	void SetWifiSwitchChecked(Boolean checked) {
		Switch checkbox = (Switch) this.findViewById(R.id.check_on_off);
		checkbox.setEnabled(true);
		checkbox.setChecked(checked);
	}
}

class Model {

	private String name;
	private String value;
	private boolean selected;

	public Model(String name, String value) {
		this.name = name;
		this.value = value;
		selected = false;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}

class ViewHolder {
	private CheckBox checkBox;
	private TextView textValueView;
	private TextView textView;

	public ViewHolder() {
	}

	public ViewHolder(TextView textView, TextView textValueView) {
		this.textView = textView;
		this.textValueView = textValueView;
	}

	public ViewHolder(TextView textView, CheckBox checkBox) {
		this.checkBox = checkBox;
		this.textView = textView;
	}

	public CheckBox getCheckBox() {
		return checkBox;
	}

	public void setCheckBox(CheckBox checkBox) {
		this.checkBox = checkBox;
	}

	public TextView getTextView() {
		return textView;
	}

	public void setTextView(TextView textView) {
		this.textView = textView;
	}

	public TextView getTextValueView() {
		return textValueView;
	}

	public void setTextValueView(TextView textValueView) {
		this.textValueView = textValueView;
	}
}

class MyCustomArrayAdapter extends ArrayAdapter<Model> {

	private final List<Model> list;
	private final Activity context;
	private LayoutInflater inflater;
	int layout;
	int row_id;
	int val_id;

	public MyCustomArrayAdapter(Activity context, List<Model> list, int layout,
			int row_id, int id) {
		super(context, R.layout.activity_main, list);
		this.context = context;
		this.list = list;

		this.layout = layout;
		this.row_id = row_id;
		this.val_id = id;

		inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Model model = (Model) this.getItem(position);

		// The child views in each row.
		// CheckBox checkBox;
		TextView textView;
		TextView textValueView;

		// Create a new row view
		if (convertView == null) {
			convertView = inflater.inflate(layout, null);

			// Find the child views.
			textView = (TextView) convertView.findViewById(row_id);
			textValueView = (TextView) convertView.findViewById(val_id);

			convertView.setTag(new ViewHolder(textView, textValueView));
		} else {
			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			textView = viewHolder.getTextView();
			textValueView = viewHolder.getTextValueView();
		}
		textView.setText(model.getName());
		textValueView.setText(model.getValue());

		convertView.setBackgroundResource(R.drawable.listview_selector);

		return convertView;
	}

	public boolean isEnabled(int position) {
		return true;// false;
	}
}