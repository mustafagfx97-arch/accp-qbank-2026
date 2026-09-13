import json, gzip, re, sys, pathlib, hashlib

SRC = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path('app/src/main/assets/questions.json.gz')
OUT = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path('web/data/questions.json.gz')

def read_json(path: pathlib.Path):
    if path.suffix == '.gz':
        with gzip.open(path, 'rt', encoding='utf-8') as f:
            return json.load(f)
    return json.loads(path.read_text(encoding='utf-8'))

data = read_json(SRC)
questions = data['questions']

# Remove scan/page section labels accidentally attached to answer choices.
footer_re = re.compile(r"\s+(?:Patient Cases?|Practice Cases?)(?:\s*\((?:Cont'?d|cont'?d)\))?\s*$", re.I)
for q in questions:
    q['stem'] = str(q.get('stem','')).strip()
    q['case_context'] = str(q.get('case_context','') or '').strip()
    q['explanation'] = str(q.get('explanation','') or '').strip()
    opts = q.get('options') or {}
    for key in list(opts):
        v = str(opts[key] or '').strip()
        opts[key] = footer_re.sub('', v).strip()
    q['options'] = opts

# Repair the known parser spill where Study Designs Q5–Q6 case text was appended to Q4 option D.
by_id = {q['id']: q for q in questions}
q4 = by_id.get('study-designs:case:4')
if q4:
    raw = q4['options'].get('D','')
    marker = re.search(r"\s+Practice Case Questions 5 and 6 pertain to the following practice case\.\s*", raw, re.I)
    if marker:
        context = raw[marker.end():].strip()
        q4['options']['D'] = raw[:marker.start()].strip()
        for qid in ('study-designs:case:5','study-designs:case:6'):
            if qid in by_id and not by_id[qid].get('case_context','').strip():
                by_id[qid]['case_context'] = context

# Full prompt is redundant on purpose: future web renderers cannot accidentally drop shared case text.
last_key = None
group_counter = 0
for idx, q in enumerate(questions):
    ctx = q.get('case_context','').strip()
    if ctx:
        key = (q['chapter_id'], q['type'], ctx)
        if key != last_key:
            group_counter += 1
        q['group_id'] = f"g{group_counter}"
        last_key = key
    else:
        group_counter += 1
        q['group_id'] = f"g{group_counter}"
        last_key = None
    q['source_order'] = idx + 1
    q['full_stem'] = (ctx + "\n\n" + q['stem']).strip() if ctx else q['stem']

data['meta']['web_rebuild'] = '2026-09-13'
data['meta']['web_full_context_mode'] = True
data['meta']['web_question_count'] = len(questions)
data['meta']['web_usable_question_count'] = sum(q.get('ocr_status') != 'source_missing' for q in questions)

assert len(questions) == 533
assert len({q['id'] for q in questions}) == 533
assert sum(q['type']=='assessment' for q in questions) == 258
assert sum(q['type']=='case' for q in questions) == 275
assert sum(q.get('ocr_status') == 'source_missing' for q in questions) == 2
assert all(q['full_stem'].strip() for q in questions)
assert all(set((q.get('options') or {}).keys()) >= {'A','B','C','D'} for q in questions)
assert all(q.get('answer') in {'A','B','C','D'} for q in questions)

payload = json.dumps(data, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
OUT.parent.mkdir(parents=True, exist_ok=True)
with OUT.open('wb') as raw:
    with gzip.GzipFile(filename='', mode='wb', fileobj=raw, compresslevel=9, mtime=0) as f:
        f.write(payload)
print('questions', len(questions))
print('usable', data['meta']['web_usable_question_count'])
print('json_sha256', hashlib.sha256(payload).hexdigest())
print('gz_bytes', OUT.stat().st_size)
