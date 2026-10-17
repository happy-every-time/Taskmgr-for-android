package com.shizuku.taskmgr

import android.os.Parcel
import android.os.Parcelable

class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(exitCode)
        parcel.writeString(output)
        parcel.writeString(error)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CommandResult> {
        override fun createFromParcel(parcel: Parcel) = CommandResult(parcel)
        override fun newArray(size: Int) = arrayOfNulls<CommandResult>(size)
    }
}