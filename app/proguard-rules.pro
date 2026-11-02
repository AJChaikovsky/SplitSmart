# SplitSmart ProGuard rules
# Keep Firestore model classes
-keepclassmembers class com.splitsmart.app.model.** {
    <init>(...);
    *;
}

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
