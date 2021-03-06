package ru.vlad805.guap.schedule.fragments;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.common.collect.Lists;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.vlad805.guap.schedule.R;
import ru.vlad805.guap.schedule.activities.DrawerActivity;
import ru.vlad805.guap.schedule.api.Groups;
import ru.vlad805.guap.schedule.api.RestApiImpl;
import ru.vlad805.guap.schedule.utils.Utils;

/**
 * Created by arktic on 08.11.15.
 */
public class SelectGroupFragment extends Fragment {

    public interface GroupSelectedListener {
        void onGroupSelected();
    }

    @Bind(R.id.select_spinner_group) Spinner spinner;

    private Utils u;
    private ProgressDialog progress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_select_group, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        u = new Utils(getContext());

        progress = u.showProgress(getString(R.string.alert_loading_groups));

        new AsyncTask<Void, Void, Groups>() {
            @Override
            protected Groups doInBackground(Void... params) {
                try {
                    return RestApiImpl.INSTANCE.getApi().getGroups().execute().body();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Groups groups) {
                if (groups != null) {
                    showItems(groups);
                } else {
                    u.toast(getString(R.string.alert_nointernet_nogroups));
                }
                progress.cancel();
            }
        }.execute();
    }

    public void showItems (Groups groups) {
        List<String> items = Lists.transform(groups.response.items, group -> group.groupId);
        ArrayAdapter<?> adapter =  new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

	    int selected = getSelectionId(items);
	    if (selected >= 0) {
		    spinner.setSelection(selected);
	    }
    }

	private int getSelectionId(List<String> items) {
		if (!u.hasString(DrawerActivity.KEY_GID)) {
			return -1;
		}

		String current = u.getString(DrawerActivity.KEY_GID);

		for (int i = 0, l = items.size(); i < l; ++i) {
			if (items.get(i).equals(current)) {
				return i;
			}
		}

		return -1;
	}

    @OnClick(R.id.select_submit)
    void onSubmit() {
        if (spinner == null) {
            u.toast(getString(R.string.alert_incorrect_group));
            return;
        }
        String group = spinner.getSelectedItem().toString();
        u.setString(DrawerActivity.KEY_GID, group);
        if (getActivity() instanceof GroupSelectedListener) {
            ((GroupSelectedListener) getActivity()).onGroupSelected();
        } else {
            throw new IllegalStateException("Activity must implement GroupSelectedListener");
        }
    }
}
