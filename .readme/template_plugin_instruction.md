{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let book = epub.open('./book.epub');
console.log(book.metadata.title, book.metadata.authors.join(', '));
book.toc.forEach(entry => console.log(entry.title));
console.log(book.text(0, { maxChars: 500 }));
book.close();
```

{{ instruction_p_script_reader }}

```js
let session = epub.read('./book.epub');
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('error', e => console.warn(e.toString()));
session.on('close', e => console.log('closed:', e.reason));
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
