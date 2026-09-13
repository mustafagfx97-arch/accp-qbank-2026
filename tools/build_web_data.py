import json, gzip, re, sys, pathlib, hashlib

SRC = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path('app/src/main/assets/questions.json.gz')
OUT = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path('web/data/questions.json.gz')

def read_json(path: pathlib.Path):
    if path.suffix == '.gz':
        with gzip.open(path, 'rt', encoding='utf-8') as f:
            return json.load(f)
    return json.loads(path.read_text(encoding='utf-8'))

def norm(s):
    return re.sub(r'\s+', ' ', str(s or '')).strip()

def patient_description(stem, question_markers):
    """Return the patient/data setup but not the trailing question, when a clean marker is known."""
    text = norm(stem)
    for marker in question_markers:
        m = re.search(marker, text, re.I)
        if m:
            return text[:m.start()].strip()
    return text

data = read_json(SRC)
questions = data['questions']
by_id = {q['id']: q for q in questions}

# Restore shared case stems that were visually present in the source PDF but
# were lost by OCR/parser segmentation. These are source-text restorations,
# not generated clinical content.
shared_contexts = {
    # Fluids, Electrolytes, and Nutrition — Patient Case Q1–2 (source p.105)
    'fluids-electrolytes-nutrition:case:1': (
        'A 65-year-old man (weight 80 kg) with a 3-day history of a body temperature of 102°F (38.9°C), lethargy, '
        'and productive cough is hospitalized for community-acquired pneumonia. His medical history includes uncontrolled '
        'hypertension and coronary artery disease. His vital signs include heart rate 104 beats/minute, blood pressure '
        '112/68 mm Hg, and body temperature 101.4°F (38.6°C). His urine output is 10 mL/hour, K+ is 4 mEq/L, BUN is '
        '46 mg/dL, SCr is 1.7 mg/dL, and WBC is 10.4 × 10^3 cells/mm³. Other laboratory values are normal.'
    ),
    'fluids-electrolytes-nutrition:case:2': (
        'A 65-year-old man (weight 80 kg) with a 3-day history of a body temperature of 102°F (38.9°C), lethargy, '
        'and productive cough is hospitalized for community-acquired pneumonia. His medical history includes uncontrolled '
        'hypertension and coronary artery disease. His vital signs include heart rate 104 beats/minute, blood pressure '
        '112/68 mm Hg, and body temperature 101.4°F (38.6°C). His urine output is 10 mL/hour, K+ is 4 mEq/L, BUN is '
        '46 mg/dL, SCr is 1.7 mg/dL, and WBC is 10.4 × 10^3 cells/mm³. Other laboratory values are normal.'
    ),

    # Pulmonology — Self-Assessment Q1–2 (source p.120)
    'pulmonology-adult-vaccinations:assessment:1': (
        'A 20-year-old woman presents to the clinic with an asthma exacerbation. She states that she has been using her '
        'boyfriend’s albuterol inhaler on a regular basis for the past 2 years. During the past few months, she has been '
        'using the inhaler on a daily basis and sometimes at night.'
    ),
    'pulmonology-adult-vaccinations:assessment:2': (
        'A 20-year-old woman presents to the clinic with an asthma exacerbation. She states that she has been using her '
        'boyfriend’s albuterol inhaler on a regular basis for the past 2 years. During the past few months, she has been '
        'using the inhaler on a daily basis and sometimes at night.'
    ),

    # Pulmonology — Patient Case Q1–2 (source p.123)
    'pulmonology-adult-vaccinations:case:1': (
        'A 23-year-old woman has been coughing and wheezing about twice weekly, and she wakes up at night about three times '
        'per month. She has never been given a diagnosis of asthma and has not been to a physician, she says, “in years.” '
        'She uses her roommate’s albuterol inhaler, but having recently run out of refills, she is seeking care. Her '
        'activities are not limited by her symptoms. Spirometry today reveals FEV1 82% of predicted.'
    ),
    'pulmonology-adult-vaccinations:case:2': (
        'A 23-year-old woman has been coughing and wheezing about twice weekly, and she wakes up at night about three times '
        'per month. She has never been given a diagnosis of asthma and has not been to a physician, she says, “in years.” '
        'She uses her roommate’s albuterol inhaler, but having recently run out of refills, she is seeking care. Her '
        'activities are not limited by her symptoms. Spirometry today reveals FEV1 82% of predicted.'
    ),

    # Oncology Supportive Care — Patient Case Q1–2 (source p.186)
    'oncology-supportive-care:case:1': (
        'A 60-year-old woman was recently given a diagnosis of advanced non–small cell lung cancer. She will begin treatment '
        'with cisplatin 100 mg/m² plus vinorelbine 30 mg/m².'
    ),
    'oncology-supportive-care:case:2': (
        'A 60-year-old woman was recently given a diagnosis of advanced non–small cell lung cancer. She will begin treatment '
        'with cisplatin 100 mg/m² plus vinorelbine 30 mg/m².'
    ),

    # Geriatrics — Self-Assessment Q1–2 (source p.281)
    'geriatrics:assessment:1': (
        'A.B., an 85-year-old man, presents to the primary care clinic 1 month after his spouse’s death. His medical history '
        'is significant for hypertension, hyperlipidemia, benign prostatic hyperplasia (BPH), and major depressive disorder. '
        'His current medications include metoprolol extended release (ER) 25 mg daily, atorvastatin 20 mg daily, tamsulosin '
        '0.4 mg daily, diazepam 5 mg at bedtime as needed for sleep, and escitalopram 10 mg daily. His daughter reports that '
        'he has been more lethargic and unsteady during walking over the past 3 days. The patient reports trouble sleeping, '
        'necessitating the use of diazepam every night this past week. His blood pressure is 135/72 mm Hg and heart rate is '
        '76 beats/minute. Urinalysis is unremarkable, thyrotropin (TSH) is within the reference range, and Geriatric '
        'Depression Scale (GDS) score is 6/15.'
    ),
    'geriatrics:assessment:2': (
        'A.B., an 85-year-old man, presents to the primary care clinic 1 month after his spouse’s death. His medical history '
        'is significant for hypertension, hyperlipidemia, benign prostatic hyperplasia (BPH), and major depressive disorder. '
        'His current medications include metoprolol extended release (ER) 25 mg daily, atorvastatin 20 mg daily, tamsulosin '
        '0.4 mg daily, diazepam 5 mg at bedtime as needed for sleep, and escitalopram 10 mg daily. His daughter reports that '
        'he has been more lethargic and unsteady during walking over the past 3 days. The patient reports trouble sleeping, '
        'necessitating the use of diazepam every night this past week. His blood pressure is 135/72 mm Hg and heart rate is '
        '76 beats/minute. Urinalysis is unremarkable, thyrotropin (TSH) is within the reference range, and Geriatric '
        'Depression Scale (GDS) score is 6/15.'
    ),

    # Study Designs — Self-Assessment Q1–2 (source p.311)
    'study-designs:assessment:1': (
        'A recently released statin is associated with less myopathy than other currently available statins. After 2 years '
        'of use, a retrospective case-control study was undertaken by the manufacturer after 20 different reports of severe '
        'myopathy were sent to the FDA MedWatch program. Risk factors for statin-induced myopathy were not assessed; however, '
        'both the cases and the controls of this study had identical diagnostic evaluations and were stratified according to '
        'the duration of statin use before the onset of myopathy.'
    ),
    'study-designs:assessment:2': (
        'A recently released statin is associated with less myopathy than other currently available statins. After 2 years '
        'of use, a retrospective case-control study was undertaken by the manufacturer after 20 different reports of severe '
        'myopathy were sent to the FDA MedWatch program. Risk factors for statin-induced myopathy were not assessed; however, '
        'both the cases and the controls of this study had identical diagnostic evaluations and were stratified according to '
        'the duration of statin use before the onset of myopathy.'
    ),

    # Study Designs — Practice Case Q5–6 (source p.317)
    'study-designs:case:5': (
        'The results of a prospective, randomized, double-blind, placebo-controlled trial show that 185/1232 patients '
        'receiving a new antithrombotic medication had a stroke, whereas 258/1230 control group patients receiving the gold '
        'standard therapy had a stroke (P < 0.05).'
    ),
    'study-designs:case:6': (
        'The results of a prospective, randomized, double-blind, placebo-controlled trial show that 185/1232 patients '
        'receiving a new antithrombotic medication had a stroke, whereas 258/1230 control group patients receiving the gold '
        'standard therapy had a stroke (P < 0.05).'
    ),
}

for qid, ctx in shared_contexts.items():
    if qid in by_id:
        by_id[qid]['case_context'] = norm(ctx)

# Restore dependent questions that explicitly point to the prior patient/data.
dependent_from_previous = {
    'chronic-care-cardiology:case:3': ('chronic-care-cardiology:case:2', [r'\s+What is the best approach.*$']),
    'sexual-reproductive-health:assessment:2': ('sexual-reproductive-health:assessment:1', [r'\s+Which best describes M\.T\.’s condition\?.*$']),
    'oncology-supportive-care:case:9': ('oncology-supportive-care:case:8', [r'\s+Which statement is most applicable\?.*$']),
    'pharmacokinetics:case:9': ('pharmacokinetics:case:8', [r'\s+Which is the best assessment of K\.M\.’s renal function\?.*$']),
    'study-designs:assessment:5': ('study-designs:assessment:4', [r'\s+Which conclusion is most appropriate\?.*$']),
    'pediatrics:case:2': ('pediatrics:case:1', [r'\s+Which is the best empiric antibiotic regimen\?.*$']),
    'pediatrics:case:12': ('pediatrics:case:11', [r'\s+Which is best for his initial drug therapy\?.*$']),
    'nephrology:case:7': ('nephrology:case:6', [r'\s+Which is best for hydration\?.*$']),
}
for qid, (src_id, markers) in dependent_from_previous.items():
    if qid in by_id and src_id in by_id and not norm(by_id[qid].get('case_context')):
        by_id[qid]['case_context'] = patient_description(by_id[src_id]['stem'], markers)

# Capture Study Designs Q5–6 case before generic label cleanup.
q4 = by_id.get('study-designs:case:4')
if q4:
    raw = norm((q4.get('options') or {}).get('D',''))
    m = re.search(r'\s+Practice Case Questions 5 and 6 pertain to the following practice case\.\s*', raw, re.I)
    if m:
        extracted = norm(raw[m.end():])
        if extracted:
            for qid in ('study-designs:case:5','study-designs:case:6'):
                if qid in by_id:
                    by_id[qid]['case_context'] = extracted
        q4['options']['D'] = raw[:m.start()].strip()

# Remove scan/page section labels accidentally attached to answer choices. If
# obvious OCR debris sits between a complete answer sentence and the page label,
# discard only that debris.
def clean_spill(value):
    v = norm(value)
    m = re.search(r'\s+(?:Patient|Practice)\s+Cases?\b', v, re.I)
    if not m:
        return v
    prefix = v[:m.start()].strip()
    punct = max(prefix.rfind('.'), prefix.rfind('?'), prefix.rfind('!'))
    if punct >= 0:
        tail = prefix[punct+1:].strip()
        if tail and (len(tail.split()) >= 2 or re.search(r'[^A-Za-z0-9%/().,;:+\-–—’\' ]', tail)):
            prefix = prefix[:punct+1].strip()
    return prefix

for q in questions:
    q['stem'] = norm(q.get('stem'))
    q['case_context'] = norm(q.get('case_context'))
    q['explanation'] = norm(q.get('explanation'))
    opts = q.get('options') or {}
    for key in list(opts):
        opts[key] = clean_spill(opts[key])
    q['options'] = opts

# Severe OCR duplicate: Patient Case 4 was appended to Case 3 option D.
if 'endocrine-metabolic:case:3' in by_id:
    by_id['endocrine-metabolic:case:3']['options']['D'] = 'Fluoxetine.'

# Harmless leading scan artifacts.
for q in questions:
    q['stem'] = re.sub(r'^<(?=[A-Za-z])', '', q['stem']).strip()
    for k,v in q['options'].items():
        q['options'][k] = re.sub(r'^_\s*', '', v).strip()

# Stable source-order/group metadata and a redundant full_stem. The renderer
# shows context separately, while full_stem prevents future context loss.
last_key = None
group_counter = 0
for idx, q in enumerate(questions):
    ctx = norm(q.get('case_context'))
    if ctx:
        key = (q['chapter_id'], q['type'], ctx)
        if key != last_key:
            group_counter += 1
        q['group_id'] = f'g{group_counter}'
        last_key = key
    else:
        group_counter += 1
        q['group_id'] = f'g{group_counter}'
        last_key = None
    q['source_order'] = idx + 1
    q['full_stem'] = f'{ctx}\n\n{q["stem"]}'.strip() if ctx else q['stem']

data['meta']['web_rebuild'] = '2026-09-13-v2'
data['meta']['web_full_context_mode'] = True
data['meta']['web_question_count'] = len(questions)
data['meta']['web_usable_question_count'] = sum(q.get('ocr_status') != 'source_missing' for q in questions)
data['meta']['web_structure_repairs'] = len(shared_contexts) + len(dependent_from_previous)

# Fail the build instead of silently publishing a corrupted bank.
expected_chapters = {c['id'] for c in data['chapters']}
assert len(data['chapters']) == 22
assert len(questions) == 533
assert len({q['id'] for q in questions}) == 533
assert sum(q['type']=='assessment' for q in questions) == 258
assert sum(q['type']=='case' for q in questions) == 275
assert sum(q.get('ocr_status') == 'source_missing' for q in questions) == 2
assert all(q['chapter_id'] in expected_chapters for q in questions)
assert all(q['full_stem'].strip() for q in questions)
assert all(set((q.get('options') or {}).keys()) >= {'A','B','C','D'} for q in questions)
assert all(q.get('answer') in {'A','B','C','D'} for q in questions)
assert all(norm(by_id[qid].get('case_context')) for qid in shared_contexts)
assert all(norm(by_id[qid].get('case_context')) for qid in dependent_from_previous)
assert 'Practice Case Questions 5 and 6' not in by_id['study-designs:case:4']['options']['D']
assert by_id['endocrine-metabolic:case:3']['options']['D'] == 'Fluoxetine.'
assert not any(re.search(r'\b(?:Patient|Practice)\s+Cases?\b', str(v), re.I)
               for q in questions for v in (q.get('options') or {}).values())

payload = json.dumps(data, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
OUT.parent.mkdir(parents=True, exist_ok=True)
with OUT.open('wb') as raw:
    with gzip.GzipFile(filename='', mode='wb', fileobj=raw, compresslevel=9, mtime=0) as f:
        f.write(payload)
print('questions', len(questions))
print('usable', data['meta']['web_usable_question_count'])
print('contexts', sum(bool(norm(q.get('case_context'))) for q in questions))
print('json_sha256', hashlib.sha256(payload).hexdigest())
print('gz_bytes', OUT.stat().st_size)
