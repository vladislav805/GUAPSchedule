package ru.vlad805.guap.schedule.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;
import ru.vlad805.guap.schedule.R;
import ru.vlad805.guap.schedule.activities.DrawerActivity;
import ru.vlad805.guap.schedule.api.RestApiImpl;
import ru.vlad805.guap.schedule.api.Schedule;
import ru.vlad805.guap.schedule.utils.Utils;
import ru.vlad805.guap.schedule.views.DayView;

public class ScheduleListFragment extends Fragment {

	@Bind(R.id.content_updated) TextView mContentUpdated;
	@Bind(R.id.content_settings) CardView mContentSettings;

	private View root;
	private Utils u;
	private int isParityNow;
	private ProgressDialog progress;
	private Schedule globalData;

	public ScheduleListFragment() {
		u = new Utils(getContext());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		u = new Utils(getContext());
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_content, container, false);
		this.root = root;
		ButterKnife.bind(this, root);
		return root;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String cache = u.getString(DrawerActivity.KEY_STORED);
		if (cache != null && !cache.isEmpty() && isAdded()) {
			Schedule data = new Gson().fromJson(cache, Schedule.class);
			init(data);
			show(data);
		}
		String groupId = u.getString(DrawerActivity.KEY_GID);
		loadAll(groupId);

	}

	public void loadAll (final String groupId) {
		progress = u.showProgress(getString(R.string.alert_updating));

		AsyncTask<Void, Void, Schedule> asyncTask = new AsyncTask<Void, Void, Schedule>() {
			@Override
			protected Schedule doInBackground(Void... params) {
				try {
					return RestApiImpl.INSTANCE.getApi().parseSchedule(groupId).execute().body();
				} catch (Exception e) {
					return null;
				}
			}
			@Override
			protected void onPostExecute(Schedule schedule) {
				if (schedule != null) {
					u.setString(DrawerActivity.KEY_STORED, new Gson().toJson(schedule));
					if (isAdded()) {
						init(schedule);
						show(schedule);
					}
				} else {
					if (u.hasString(DrawerActivity.KEY_STORED)) {
						u.toast(getString(R.string.alert_nointernet));
					} else {
						u.toast(getString(R.string.alert_nointernet_nothing2show));
					}
				}
				progress.cancel();
			}
		}.execute();
	}

	//нахуй так жить TODO: ListView
	public void init (Schedule data) {
		mContentUpdated.setText(String.format(getString(R.string.schedule_from), data.response.parseDate));

		mContentSettings.setContentPadding(DayView.PADDING_LR, DayView.PADDING_TB, DayView.PADDING_LR, DayView.PADDING_TB);

		LinearLayout.LayoutParams lp = DayView.getDefaultLayoutParams(DayView.MATCH_PARENT, DayView.WRAP_CONTENT);
		lp.setMargins(DayView.MARGIN_LR, DayView.MARGIN_TB, DayView.MARGIN_LR, DayView.MARGIN_TB);
		mContentSettings.setLayoutParams(lp);
		mContentSettings.setVisibility(View.VISIBLE);

		isParityNow = (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2) + 1;

		Switch sw = (Switch) getActivity().findViewById(R.id.switcher_parity);

		sw.setChecked(isParityNow == 2);

		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isParityNow = !isChecked ? 1 : 2;
				show(globalData);
			}
		});
	}

	//нахуй так жить TODO: ListView
	public void show (Schedule data) {

		globalData = data;

		Activity act = getActivity();
		LinearLayout list = new LinearLayout(act);
		list.setOrientation(LinearLayout.VERTICAL);
		DayView itemLayout;

		int l = data.response.schedule.size();

		for (byte j = 0; j < l; ++j) {
			itemLayout = new DayView(act);
			itemLayout.setDay(data, j, isParityNow);

			list.addView(itemLayout);
		}

		LinearLayout parent = ((LinearLayout) act.findViewById(R.id.content_wrap));

		if (parent.getChildCount() > 0) {
			parent.removeAllViews();
		}
		parent.addView(list);
	}

}
