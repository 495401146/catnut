/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;
import org.catnut.core.CatnutMetadata;

/**
 * 草稿
 *
 * @author longkai 
 */
public class Draft implements CatnutMetadata<Void, Void>, Parcelable {

	public static final String DRAFT = "Draft";
	public static final String TABLE = "drafts";
	public static final String SINGLE = "draft";
	public static final String MULTIPLE = "drafts";

	public static final String STATUS = "status";
	public static final String VISIBLE = "visiable";
	public static final String LIST_ID = "list_id";
	public static final String _LONG = "_long";
	public static final String LAT = "lat";
	public static final String ANNOTATIONS = "annotations";
	public static final String RIP = "rip";
	public static final String PIC = "pic";

	public static final Draft METADATA = new Draft();

	private Draft(){}

	public String status;
	public int visible = 0;
	public String list_id;
	public float lat = 0.f;
	public float _long = 0.f;
	public String annotations;
	public String rip;
	public Uri pic;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(status);
		dest.writeInt(visible);
		dest.writeString(list_id);
		dest.writeFloat(lat);
		dest.writeFloat(_long);
		dest.writeString(annotations);
		dest.writeString(rip);
		dest.writeParcelable(pic, flags);
	}

	public static final Parcelable.Creator<Draft> CREATOR = new Parcelable.Creator<Draft>() {
		@Override
		public Draft createFromParcel(Parcel source) {
			Draft weibo = new Draft();
			weibo.status = source.readString();
			weibo.visible = source.readInt();
			weibo.list_id = source.readString();
			weibo.lat = source.readFloat();
			weibo._long = source.readFloat();
			weibo.annotations = source.readString();
			weibo.rip = source.readString();
			weibo.pic = source.readParcelable(Thread.currentThread().getContextClassLoader());
			return weibo;
		}

		@Override
		public Draft[] newArray(int size) {
			return new Draft[size];
		}
	};

	@Override
	public String ddl() {
		Log.i(TABLE, "create table [statuses]...");

		StringBuilder ddl = new StringBuilder("CREATE TABLE ");
		ddl.append(TABLE).append("(")
				.append(BaseColumns._ID).append(" int primary key,")
				.append(STATUS).append(" text,")
				.append(_LONG).append(" text,")
				.append(LAT).append(" text,")
				.append(ANNOTATIONS).append(" text,")
				.append(VISIBLE).append(" int,")
				.append(LIST_ID).append(" text,")
				.append(RIP).append(" text,")
				.append(PIC).append(" text")
				.append(")");
		return ddl.toString();
	}

	@Override
	public Void convert(Void data) {
		return null;
	}
}
