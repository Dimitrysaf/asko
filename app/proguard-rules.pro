# Project-specific R8 rules. Room, kotlinx.serialization, Coil and Lottie ship
# their own consumer rules, so nothing is needed here yet.

# MediaPipe (the camera rep counter) is driven from its native code, which finds its Java classes
# and protobuf messages by name: shrinking must leave them alone.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
