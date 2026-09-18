<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>قراءة كتب EPUB مع التنقل والبحث والقراءة الصوتية والوصول من السكربتات</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

قراءة بنقرة واحدة: افتح ملف `.epub` مباشرة من مدير ملفات AutoJs6, سواء بالزر الرئيسي `قراءة EPUB` أو من القائمة. يعتمد القارئ على [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0, وهو المحرك مفتوح المصدر الذي تستخدمه قارئات تجارية كثيرة.

يقرأ الملحق الكتاب مباشرة عبر واصف الملف المؤقت الذي يمنحه المضيف. لا يحصل أبدا على مسار نظام الملفات, ولا ينسخ الكتاب إلى أي مكان, ولا يستخرجه إلى التخزين.

> المرحلة الحالية (بنية تطويرية 1.0.0): يفتح القارئ الكتاب بإعدادات Readium الافتراضية ويوفر جدول المحتويات. حفظ موضع القراءة, الإشارات المرجعية, التفضيلات, البحث في النص الكامل, القراءة الصوتية, التخطيط الثابت, استيراد الخطوط, مدخل المشغل المستقل وواجهة السكربت `epub` مخطط لها في ROADMAP.md وغير متاحة بعد.

******

### أبرز الميزات

******

- محرك Readium: تعرض كتب EPUB 2 (NCX) و EPUB 3 (NAV) عبر متصفح Readium مع Readium CSS, بما في ذلك الروابط الداخلية والحواشي والصور.
- بلا نسخ: تقرأ حاوية EPUB في مكانها عبر واصف للقراءة فقط بقراءات موضعية, لذلك تفتح حتى الكتب الكبيرة دون ملف ذاكرة مؤقتة.
- جدول المحتويات: انتقل إلى أي فصل من شريط الأدوات; تحافظ المدخلات المتداخلة على مستواها.
- الروابط الخارجية: النقر على رابط `http` أو `https` يعرض العنوان الكامل ولا يفتح متصفح النظام إلا بعد التأكيد.
- التكامل مع المضيف: تتبع القوائم والحوارات لغة AutoJs6 والوضع الداكن; يتم التحقق من مغلف Explorer Action بدقة قبل فتح أي محتوى.
- متعدد اللغات: الواجهة والتعليمات و README وسجل التغييرات متاحة بعشر لغات.

******

### طريقة الاستخدام

******

1. نزل أحدث APK للملحق من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) وثبته على الجهاز.
2. افتح مركز الملحقات في AutoJs6 وفعل ملحق `Readium EPUB Reader`.
3. في مدير ملفات AutoJs6 انقر على ملف `.epub`, أو افتح قائمته (إجراءات أخرى) واختر `قراءة EPUB`.
4. استخدم زر جدول المحتويات في شريط الأدوات للتنقل بين الفصول; اضغط رجوع لإغلاق القارئ.

> إذا لم يظهر الملحق في مركز الملحقات فحدث AutoJs6 أولا إلى إصدار حديث (البنية الداخلية 5269 أو أحدث). يدعم Explorer Action v2 الزر الرئيسي وقائمة الملف الواحد مع إذن قراءة مؤقت للمستند والمجلد الأب.

******

### التنسيقات المدعومة

******

يتعرف الملحق على الامتداد التالي, وعلى الملفات بلا امتداد التي يحددها المضيف صراحة كـ `application/epub+zip`:

```text
epub
```

يدعم EPUB فقط: الكتب القابلة لإعادة التدفق وذات التخطيط الثابت في EPUB 2 أو EPUB 3. أرشيفات القصص المصورة (CBZ) والكتب الصوتية و PDF والكتب المحمية بـ LCP خارج النطاق; الكتاب المعلم كمشفر بـ LCP يبلغ عنه كغير قابل للقراءة بدلا من عرض محتوى تالف.

******

### الأسئلة الشائعة

******

#### لماذا لا يحفظ موضع القراءة بعد?

حفظ موضع القراءة والإشارات المرجعية ينتميان إلى المرحلة التالية في ROADMAP.md. تفتح البنية الحالية الكتاب دائما من بدايته.

#### هل يمكنني تغيير الخط أو حجم النص أو المظهر?

ليس بعد. تفضيلات القراءة (الخط, الحجم, تباعد الأسطر, الهوامش, المظاهر, وضع الصفحات أو التمرير) تأتي مع مرحلة التفضيلات; تستخدم البنية الحالية إعدادات Readium الافتراضية.

#### هل يرفع هذا الملحق كتبي إلى أي مكان?

لا. ليس للملحق خادم خاص به. تستخدم الشبكة فقط عندما يشير الكتاب نفسه إلى موارد بعيدة, وللتحقق اليدوي من التحديثات المخطط له في صفحة الإعدادات المستقلة.

******

### الأذونات والأمان

******

يحتفظ الملحق بسلوك Readium الافتراضي لمحتوى الكتاب: لا تزال السكربتات والموارد البعيدة داخل الكتاب ولا تحجب, بما في ذلك موارد `http://` غير المشفرة. افتح الكتب الموثوقة فقط.

- أقل امتياز: يتلقى الملحق فقط إذن القراءة المؤقت لـ content URI الممنوح من المضيف, ولا يرى مسارات نظام الملفات أبدا, ولا يكتب الكتاب إلى التخزين.
- مغلف صارم: يجب أن يحمل طلب Explorer Action هدف EPUB واحدا بالضبط, ومجلده الأب, وإصدار بروتوكول مطابقا, وبنية مضيف مدعومة, وإذني القراءة معا; يرفض أي شيء آخر قبل فتح الملف.
- تحليل محدود: الحاوية التالفة (ليست ZIP, أو بلا `container.xml`, أو بلا مستند الحزمة, أو اجتياز مسار في manifest) تنتهي برسالة خطأ بدلا من الانهيار.
- تعرض الروابط الخارجية كاملة ولا تفتح في متصفح النظام إلا بعد التأكيد; ترفض الأنظمة غير `http` و `https`.

يطلب البيان إذن الشبكة وإذن ملحق AutoJs6 فقط. يضيف AndroidX أيضا إذن توقيع محدود بالحزمة يحمي المستقبلات الديناميكية غير المصدرة; ولا يمنح أي وصول إلى بيانات الجهاز. لا يطلب أي إذن للتخزين أو الوسائط أو الكاميرا أو الموقع أو إمكانية الوصول أو التراكب.

******

### واجهة الملحق

******

المعلومات التالية موجهة للمطورين. يكتشف المضيف الملحق وينفذه بهذه المعرفات:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

يدعم Explorer Action v2 الزر الرئيسي وقائمة الملف الواحد مع إذن قراءة مؤقت للمستند والمجلد الأب. يلزم AutoJs6 بالإصدار الداخلي 5269 أو أحدث.

- [عرض مصفوفة توافق Explorer Action](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### خارطة الطريق

******

تتبع القدرات المخططة وحالة إنجازها في ROADMAP.md كقائمة قابلة للتحقق, منظمة حسب المراحل مع معايير القبول: حفظ موضع القراءة والإشارات المرجعية, التفضيلات واستيراد الخطوط, البحث في النص الكامل, القراءة الصوتية, التخطيط الثابت, مدخل التطبيق المستقل, عقد المضيف وواجهة السكربت `epub`. تصف العناصر غير المحددة خططا وليس قدرات متاحة. نرحب بالملاحظات عبر Issues.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/09/19_

- `تلميح` بنية تطويرية: مراحل P0 في خارطة الطريق (الهيكل, التحقق من Readium, عينات الاختبار) قيد التنفيذ; يصدر أول إصدار عام مع مرحلة P8 في خارطة الطريق
- `ميزة` زر رئيسي `قراءة EPUB` وإجراء في القائمة لملفات `.epub` في مدير ملفات AutoJs6 (معرف الملحق `readium-epub-reader`, Explorer Action v2)
- `ميزة` أساس القارئ: تعرض كتب EPUB 2 و EPUB 3 عبر متصفح Readium, مع جدول محتويات وروابط خارجية تتطلب التأكيد
- `ميزة` تقرأ الكتب في مكانها عبر واصف الملف الممنوح بقراءات موضعية; لا ينسخ أو يستخرج أي شيء إلى التخزين
- `ميزة` الواجهة والتعليمات و README وسجل التغييرات بعشر لغات
- `تبعية` إضافة Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

```powershell
.\gradlew.bat :app:assembleDebug
```

بناء Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

تأتي معلمات البناء من `version.properties`. الحد الأدنى الحالي لـ SDK هو 24 والـ SDK المستهدف هو 37.

******

### الترجمة وتوليد الوثائق

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

يوفر `strings.xml` ترجمة معلومات الملحق وواجهة القارئ بينما يوفر `plugin_instruction.md` تعليمات الاستخدام التي يعرضها المضيف. لتعديل README وسجل التغييرات عدل دائما مصادر JSON في `.readme/` و `.changelog/` ثم شغل `py .python/generate_markdown.py` لإعادة التوليد; الملفات المولدة لا تحرر يدويا أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من تزامن المصادر والملفات المولدة.

******

### الروابط

******

- وثائق AutoJs6: https://docs.autojs6.com
- مواصفة EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
