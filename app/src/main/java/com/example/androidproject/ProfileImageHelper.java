package com.example.androidproject;

import java.util.Arrays;
import java.util.List;

public class ProfileImageHelper {
    private static final List<Integer> LOCAL_PROFILE_IMAGES = Arrays.asList(
            R.drawable.tofu,
            R.drawable.profile1,
            R.drawable.profile2,
            R.drawable.profile3
    );

    private static final List<String> IMAGE_NAMES = Arrays.asList(
            "Default",
            "Profile 1",
            "Profile 2",
            "Profile 3"
    );

    public static List<Integer> getLocalProfileImages() {
        return LOCAL_PROFILE_IMAGES;
    }

    public static List<String> getImageNames() {
        return IMAGE_NAMES;
    }

    public static String getImageName(int drawableResId) {
        int index = LOCAL_PROFILE_IMAGES.indexOf(drawableResId);
        return index >= 0 ? IMAGE_NAMES.get(index) : "Default";
    }

    public static int getImageResource(String imageName) {
        int index = IMAGE_NAMES.indexOf(imageName);
        return index >= 0 ? LOCAL_PROFILE_IMAGES.get(index) : R.drawable.tofu;
    }
}