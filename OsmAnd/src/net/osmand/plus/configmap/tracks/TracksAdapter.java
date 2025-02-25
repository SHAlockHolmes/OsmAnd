package net.osmand.plus.configmap.tracks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.NoVisibleTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.RecentlyVisibleViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.UpdateLocationViewCache;
import net.osmand.util.Algorithms;

import java.util.Set;

public class TracksAdapter extends RecyclerView.Adapter<ViewHolder> {

	// values are used to sort items in TracksComparator
	public static final int TYPE_SORT_TRACKS = 0;
	public static final int TYPE_NO_TRACKS = 1;
	public static final int TYPE_NO_VISIBLE_TRACKS = 2;
	public static final int TYPE_RECENTLY_VISIBLE_TRACKS = 3;
	public static final int TYPE_TRACK = 4;

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache locationViewCache;
	private final TrackTab trackTab;
	private final TracksFragment fragment;
	private final boolean nightMode;

	public TracksAdapter(@NonNull OsmandApplication app, @NonNull TrackTab trackTab, @NonNull TracksFragment fragment, boolean nightMode) {
		this.app = app;
		this.uiUtilities = app.getUIUtilities();
		this.trackTab = trackTab;
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.locationViewCache = uiUtilities.getUpdateLocationViewCache();
		locationViewCache.arrowResId = R.drawable.ic_direction_arrow;
		locationViewCache.arrowColor = ColorUtilities.getActiveIconColorId(nightMode);
	}

	@NonNull
	public TrackTab getTrackTab() {
		return trackTab;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_TRACK:
				View view = inflater.inflate(R.layout.track_list_item, parent, false);
				return new TrackViewHolder(view, fragment, locationViewCache, nightMode);
			case TYPE_NO_TRACKS:
				view = inflater.inflate(R.layout.empty_state, parent, false);
				return new EmptyTracksViewHolder(view, fragment, nightMode);
			case TYPE_NO_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.empty_state, parent, false);
				return new NoVisibleTracksViewHolder(view, fragment, nightMode);
			case TYPE_RECENTLY_VISIBLE_TRACKS:
				view = inflater.inflate(R.layout.list_header_switch_item, parent, false);
				return new RecentlyVisibleViewHolder(view, fragment, nightMode);
			case TYPE_SORT_TRACKS:
				view = inflater.inflate(R.layout.sort_type_view, parent, false);
				return new SortTracksViewHolder(view, fragment, nightMode);
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = trackTab.items.get(position);
		if (object instanceof TrackItem) {
			return TYPE_TRACK;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_NO_TRACKS == item) {
				return TYPE_NO_TRACKS;
			} else if (TYPE_NO_VISIBLE_TRACKS == item) {
				return TYPE_NO_VISIBLE_TRACKS;
			} else if (TYPE_RECENTLY_VISIBLE_TRACKS == item) {
				return TYPE_RECENTLY_VISIBLE_TRACKS;
			} else if (TYPE_SORT_TRACKS == item) {
				return TYPE_SORT_TRACKS;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (holder instanceof TrackViewHolder) {
			TrackItem item = (TrackItem) trackTab.items.get(position);
			boolean hideDivider = position == getItemCount() - 1
					|| Algorithms.objectEquals(trackTab.items.get(position + 1), TYPE_RECENTLY_VISIBLE_TRACKS);
			TrackViewHolder viewHolder = (TrackViewHolder) holder;
			viewHolder.bindView(this, item, !hideDivider);
		} else if (holder instanceof NoVisibleTracksViewHolder) {
			((NoVisibleTracksViewHolder) holder).bindView();
		} else if (holder instanceof EmptyTracksViewHolder) {
			((EmptyTracksViewHolder) holder).bindView();
		} else if (holder instanceof RecentlyVisibleViewHolder) {
			((RecentlyVisibleViewHolder) holder).bindView();
		} else if (holder instanceof SortTracksViewHolder) {
			((SortTracksViewHolder) holder).bindView(trackTab);
		}
	}

	public void ontrackItemsSelected(@NonNull Set<TrackItem> trackItems) {
		for (TrackItem trackItem : trackItems) {
			updateItem(trackItem);
		}
		updateItem(TYPE_RECENTLY_VISIBLE_TRACKS);
	}

	private void updateItem(@NonNull Object object) {
		int index = trackTab.items.indexOf(object);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	@Override
	public int getItemCount() {
		return trackTab.items.size();
	}
}