package com.banuba.sdk.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Size;

public interface SizeProvider { Size provide(@NonNull Size viewportSize); }
