# Korean Flashcards — Native Android App

এটা একটা সম্পূর্ণ native Kotlin + Jetpack Compose app যেটা সরাসরি তোমার
Supabase database এর সাথে কথা বলে (কোনো WebView/browser লাগে না)। তোমার
web app এ যে Flashcard feature আছে, ঠিক সেই Leitner box logic এখানে
native ভাবে implement করা।

## ধাপ ১: নতুন প্রজেক্ট বানাও
Android Studio তে:
- New Project → **Empty Activity** (এটা এখন default ভাবে Jetpack Compose ব্যবহার করে)
- Name: `Korean Flashcards`
- Package name: `com.jubaer.koreanflashcards` (এই নামেই সব ফাইল লেখা হয়েছে; ভিন্ন
  নাম দিতে চাইলে প্রতিটা `.kt` ফাইলের প্রথম লাইনের package name বদলে দিও)
- Language: **Kotlin**
- Minimum SDK: **API 24**

## ধাপ ২: ফাইলগুলো বসাও
এই zip এর `app/src/main/java/com/jubaer/koreanflashcards/` এর ভিতরের সবগুলো `.kt`
ফাইল (৭টা) তোমার project এর একই path এ কপি করো (replace/add করো):

- `SupabaseConfig.kt`
- `ApiModels.kt`
- `SupabaseApi.kt`
- `ApiClient.kt`
- `DateUtils.kt`
- `FlashcardRepository.kt`
- `FlashcardViewModel.kt`
- `MainActivity.kt` (replace করো, wizard যেটা বানিয়ে দিয়েছিল সেটার বদলে)

আর:
- `AndroidManifest.xml` → replace করো (INTERNET permission যোগ করা আছে)
- `res/values/strings.xml` → replace করো

## ধাপ ৩: Dependencies যোগ করো
`app/build.gradle.kts` ফাইল খুলে `build.gradle.dependencies.txt` এ যা যা লেখা
আছে সেগুলো `dependencies { }` ব্লকের ভিতরে যোগ করো। যোগ করার পর উপরে
**"Sync Now"** বাটনে চাপো (Android Studio automatically দেখাবে)।

## ধাপ ৪: Run করো
Phone USB দিয়ে connect করে (Developer Options + USB debugging চালু করে)
উপরে **Run ▶️** চাপো। App খুলে সরাসরি Chapter বেছে "Session শুরু করো" চাপলেই
তোমার Supabase database থেকে due card গুলো নিয়ে আসবে।

## ধাপ ৫: Release APK বানাও
**Build → Generate Signed Bundle/APK → APK** → নতুন keystore বানাও (password
মনে রেখো) → **release** variant → Finish। `app/release/app-release.apk`
ফাইলটা phone এ install করা যাবে।

## এই app যা করে
- **🎴 Flashcards tab:**
  - Chapter অনুযায়ী ফিল্টার (dropdown)
  - আজকে review করার মতো word (Leitner box অনুযায়ী) দেখাবে
  - "উত্তর দেখাও" → Bangla meaning দেখাবে
  - "✅ ঠিক বলেছিলাম" / "❌ জানতাম না" → box level আপডেট হয়ে সরাসরি
    Supabase এ save হবে (web app এর data এর সাথেই sync থাকবে)
  - Session শেষে সারাংশ (কতগুলো সঠিক/ভুল)
- **📚 Vocabulary tab:**
  - Chapter বেছে সেই chapter এর সব word (Korean + Bangla) list আকারে দেখা

নিচে bottom navigation bar দিয়ে দুটো tab এর মধ্যে switch করা যাবে।

## সীমাবদ্ধতা
- Internet connection লাগবে (Supabase cloud database)
- এই version এ শুধু Flashcard feature আছে — বাকি feature (Upload, Question
  Bank ইত্যাদি) চাইলে ধাপে ধাপে যোগ করা যাবে
