package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_DESCENDING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.Comparator;

public class TracksComparator implements Comparator<Object> {

	public final LatLon latLon;
	public final TrackTab trackTab;
	public final TracksSortMode sortMode;
	public final Collator collator = OsmAndCollator.primaryCollator();

	public TracksComparator(@NonNull TrackTab trackTab, @NonNull LatLon latLon) {
		this.trackTab = trackTab;
		this.sortMode = trackTab.getSortMode();
		this.latLon = latLon;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Integer) {
			return o2 instanceof Integer ? Integer.compare((Integer) o1, (Integer) o2) : -1;
		}
		if (o2 instanceof Integer) {
			return 1;
		}
		if (o1 instanceof TrackItem && o2 instanceof TrackItem) {
			return compareTrackItems((TrackItem) o1, (TrackItem) o2);
		}
		return 0;
	}

	private int compareTrackItems(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		GpxDataItem dataItem1 = item1.getDataItem();
		GpxDataItem dataItem2 = item2.getDataItem();
		GPXTrackAnalysis analysis1 = dataItem1 != null ? dataItem1.getAnalysis() : null;
		GPXTrackAnalysis analysis2 = dataItem2 != null ? dataItem2.getAnalysis() : null;

		if (shouldCheckAnalisis()) {
			Integer value = checkItemsAnalysis(item1, item2, analysis1, analysis2);
			if (value != null) {
				return value;
			}
		}

		switch (sortMode) {
			case NEAREST:
				return compareNearestItems(item1, item2, analysis1, analysis2);
			case NAME_ASCENDING:
				return compareTrackItemNames(item1, item2);
			case NAME_DESCENDING:
				return -compareTrackItemNames(item1, item2);
			case DATE_ASCENDING:
				if (analysis1.startTime == analysis2.startTime) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(analysis1.startTime, analysis2.startTime);
			case DATE_DESCENDING:
				if (analysis1.startTime == analysis2.startTime) {
					return compareTrackItemNames(item1, item2);
				}
				return Long.compare(analysis1.startTime, analysis2.startTime);
			case LAST_MODIFIED:
				return compareItemFilesByLastModified(item1, item2);
			case DISTANCE_DESCENDING:
				if (analysis1.totalDistance == analysis2.totalDistance) {
					return compareTrackItemNames(item1, item2);
				}
				return -Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DISTANCE_ASCENDING:
				if (analysis1.totalDistance == analysis2.totalDistance) {
					return compareTrackItemNames(item1, item2);
				}
				return Float.compare(analysis1.totalDistance, analysis2.totalDistance);
			case DURATION_DESCENDING:
				if (analysis1.timeSpan == analysis2.timeSpan) {
					return compareTrackItemNames(item1, item2);
				}
				return -Long.compare(analysis1.timeSpan, analysis2.timeSpan);
			case DURATION_ASCENDING:
				if (analysis1.timeSpan == analysis2.timeSpan) {
					return compareTrackItemNames(item1, item2);
				}
				return Long.compare(analysis1.timeSpan, analysis2.timeSpan);
		}
		return 0;
	}

	private boolean shouldCheckAnalisis() {
		return sortMode != NAME_ASCENDING && sortMode != NAME_DESCENDING && sortMode != LAST_MODIFIED;
	}

	@Nullable
	private Integer checkItemsAnalysis(@NonNull TrackItem item1, @NonNull TrackItem item2,
	                                   @Nullable GPXTrackAnalysis analysis1, @Nullable GPXTrackAnalysis analysis2) {
		if (analysis1 == null) {
			return analysis2 == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (analysis2 == null) {
			return -1;
		}
		return null;
	}

	private int compareNearestItems(@NonNull TrackItem item1, @NonNull TrackItem item2,
	                                @Nullable GPXTrackAnalysis analysis1, @Nullable GPXTrackAnalysis analysis2) {
		if (analysis1.latLonStart == null) {
			return analysis2.latLonStart == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (analysis2.latLonStart == null) {
			return -1;
		}
		if (analysis1.latLonStart.equals(analysis2.latLonStart)) {
			return compareTrackItemNames(item1, item2);
		}
		double distance1 = MapUtils.getDistance(latLon, analysis1.latLonStart);
		double distance2 = MapUtils.getDistance(latLon, analysis2.latLonStart);
		return Double.compare(distance1, distance2);
	}

	private int compareItemFilesByLastModified(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		File file1 = item1.getFile();
		File file2 = item2.getFile();

		if (file1 == null) {
			return file2 == null ? compareTrackItemNames(item1, item2) : 1;
		}
		if (file2 == null) {
			return -1;
		}
		if (file1.lastModified() == file2.lastModified()) {
			return compareTrackItemNames(item1, item2);
		}
		return -Long.compare(file1.lastModified(), file2.lastModified());
	}

	private int compareTrackItemNames(@NonNull TrackItem item1, @NonNull TrackItem item2) {
		if (trackTab.type == TrackTabType.FOLDER) {
			return collator.compare(item1.getName(), item2.getName());
		} else {
			return collator.compare(item1.getPath(), item2.getPath());
		}
	}
}
