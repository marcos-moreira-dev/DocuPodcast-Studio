"""Cross human DOCX dialogue with project segments without changing speech text.

Analysis is read-only; reviewed decisions are applied separately with backups.
"""
from __future__ import annotations

import argparse
import csv
from collections import Counter, defaultdict
import difflib
import json
import hashlib
from pathlib import Path
import re
import shutil
from datetime import datetime
import unicodedata
import xml.etree.ElementTree as ET
import zipfile


def normalized(text: str) -> str:
    """Ignore layout, accents and punctuation, retaining all letters/numbers."""
    return ''.join(c.lower() for c in unicodedata.normalize('NFD', text)
                   if c.isalnum())


def paragraphs(path: Path) -> list[str]:
    """Read visible Word paragraphs in document order without executing content."""
    with zipfile.ZipFile(path) as archive:
        root = ET.fromstring(archive.read('word/document.xml'))
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    return [''.join(t.text or '' for t in p.findall('.//w:t', ns))
            for p in root.findall('.//w:p', ns)]


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding='utf-8-sig'))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prefix(text: str) -> str:
    return re.split(r'[\w¿¡]', text, maxsplit=1)[0].strip(' .…\t')


def prepare(project_file: Path, word: Path, output: Path, rules_file: Path, runtime: Path) -> None:
    """Resolve repeated dialogue in order and record every editorial decision."""
    rows = read_json(output/'cross.json')
    rules = read_json(rules_file)
    project = read_json(project_file)
    aliases = {v['characterId']: v['voiceProfileId'] for v in project['theatre']['voiceRoleAliases']}
    samples = {v['voiceProfileId']: {s['tone']: s for s in v['samples']}
               for v in project['voiceLibrary']['referenceSampleSets']}
    choruses = {v['intervencionId']: v['participantCharacterIds'] for v in project['theatre']['choralVoiceAssignments']}
    previous = 0
    decisions = []
    for row in rows:
        possible = [s for s in row['matches'] if s['paragraph'] > previous]
        if not possible:
            raise ValueError(f"No exact ordered match: {row['intervention']}")
        match = min(possible, key=lambda s: s['paragraph'])
        previous = match['paragraph']
        emojis = prefix(match['text'])
        override = rules['overrides'].get(row['intervention'])
        tone = rules['combinations'].get(emojis)
        reason = 'Equivalencia editorial del gesto del guion; no inferida por el motor TTS.'
        if not tone:
            tone = next((rules['gestures'][c] for c in emojis if c in rules['gestures']), None)
        if override:
            tone, reason = override
        if not tone:
            raise ValueError(f"Unreviewed emoji combination: {row['intervention']} {emojis}")
        voice = aliases.get(row['characterId'], '')
        sample = samples.get(voice, {}).get(tone)
        sample_path = None
        if sample:
            sample_path = Path(sample['fileUri'])
            if not sample_path.is_absolute():
                sample_path = (runtime if sample['ownership'] == 'APP_RESOURCE' else project_file.parent)/sample_path
        member_references = []
        for member in choruses.get(row['intervention'], []):
            member_voice = aliases[member]
            member_sample = samples.get(member_voice, {}).get(tone)
            if not member_sample:
                raise ValueError(f'Missing chorus sample: {member} {tone}')
            member_path = Path(member_sample['fileUri'])
            if not member_path.is_absolute():
                member_path = (runtime if member_sample['ownership'] == 'APP_RESOURCE' else project_file.parent)/member_path
            member_references.append({'character': member, 'voice': member_voice, 'path': str(member_path), 'exists': member_path.is_file()})
        decisions.append({
            'intervention': row['intervention'], 'segment': row['segment'],
            'characterId': row['characterId'], 'scene': row['scene'], 'text': row['text'],
            'paragraph': match['paragraph'], 'human_text': match['text'], 'emoji': emojis,
            'before': match['before'], 'after': match['after'],
            'tone': tone, 'reason': reason, 'contextual_override': bool(override),
            'voice': voice, 'reference': str(sample_path) if sample_path else '',
            'chorus_references': member_references,
            'reference_exists': all(m['exists'] for m in member_references) if member_references else bool(sample_path and sample_path.is_file()),
            'review': 'Revisar mezcla de gestos' if not override and len([c for c in emojis if c in rules['gestures']]) > 1 else ''})
    payload = {'project': str(project_file.resolve()), 'project_sha256': sha(project_file),
               'script_sha256': sha(project_file.parent/'script/narration-script.json'),
               'source': str(word.resolve()), 'source_sha256': sha(word), 'decisions': decisions}
    (output/'decisions.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    fields = ['intervention','characterId','scene','paragraph','emoji','tone','voice','reference_exists','review','reason','text']
    with (output/'decisions.csv').open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.DictWriter(stream, fields, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(decisions)
    print(json.dumps({'tones': Counter(d['tone'] for d in decisions),
                      'missing_reference': [d['intervention'] for d in decisions if not d['reference_exists']],
                      'mixed_gestures': sum(bool(d['review']) for d in decisions)}, ensure_ascii=False))


def apply_decisions(project_file: Path, output: Path) -> None:
    """Apply reviewed data to theatre states and playback layers, with backup."""
    plan = read_json(output/'decisions.json')
    script_path = project_file.parent/'script/narration-script.json'
    if sha(project_file) != plan['project_sha256'] or sha(script_path) != plan['script_sha256']:
        raise ValueError('Project changed after review; regenerate the cross before applying.')
    project = read_json(project_file)
    script = read_json(script_path)
    segments = {s['id']: s for s in script['segments']}
    states = {s['interventionId']: s for s in project['theatre']['interventionStates']}
    assignments = project['narrativeLayers']['assignments']
    if any(a['kind'] == 'EMOTION' for a in assignments) or any(s['tone'] for s in states.values()):
        raise ValueError('Existing emotions require explicit merge review; nothing was overwritten.')
    for decision in plan['decisions']:
        state = states[decision['intervention']]
        segment = segments[decision['segment']]
        if segment['narrationText'] != decision['text']:
            raise ValueError('Speech mismatch')
        state['tone'] = decision['tone']
        state['emoji'] = decision['emoji']
        assignments.append({'id': 'NLA-EMOTION-CROSS-'+decision['intervention'], 'kind': 'EMOTION',
                            'segmentId': segment['id'], 'startOffset': 0,
                            'endOffset': len(segment['narrationText'].encode('utf-16-le'))//2,
                            'targetId': 'TONE-'+decision['tone'], 'displayName': 'Tono '+decision['tone'],
                            'notes': f"Cruce DOCX, párrafo {decision['paragraph']}. {decision['emoji']} {decision['reason']}"})
    backup = project_file.parent.parent / ('emotion-backup-'+datetime.now().strftime('%Y%m%d-%H%M%S'))
    backup.mkdir()
    shutil.copy2(project_file, backup/project_file.name)
    # Keep speech, voices, jobs and audio bytes untouched. Only operational JSON changes.
    encoded = json.dumps(project, ensure_ascii=False, indent=2)+'\n'
    temporary = project_file.with_suffix('.emotion-tmp')
    temporary.write_text(encoded, encoding='utf-8')
    temporary.replace(project_file)
    evidence = project_file.parent/'diagnostics'/'emotion-cross'
    shutil.copytree(output, evidence, dirs_exist_ok=True)
    print(json.dumps({'applied': len(plan['decisions']), 'backup': str(backup), 'report': str(evidence)}, ensure_ascii=False))


def sync_grammar(project_file: Path, output: Path, backup: Path) -> None:
    """Keep source grammar metadata consistent, without changing dialogue lines."""
    decisions = {d['intervention']: d for d in read_json(output/'decisions.json')['decisions']}
    root = project_file.parent
    paths = list((root/'source').glob('*.md')) + list((root/'assets/theatre').glob('*/obra.teatro.md'))
    for path in paths:
        original = path.read_text(encoding='utf-8-sig')
        seen = set()
        updated = []
        for line in original.splitlines(keepends=True):
            match = re.search(r'(?:^|\|)\s*(?:>\s*)?id=(INTERVENCION-\d+)(?=\s|\||$)', line)
            if match and line.lstrip().startswith('>') and match[1] in decisions:
                d = decisions[match[1]]
                seen.add(match[1])
                ending = '\n' if line.endswith('\n') else ''
                line = re.sub(r'\s*\|\s*(?:tono|emoji)=[^|\r\n]*', '', line.rstrip('\r\n'))
                line += f" | tono={d['tone']} | emoji={d['emoji']}"+ending
            updated.append(line)
        if seen != set(decisions):
            raise ValueError(f'Incomplete grammar: {path}, found {len(seen)}')
        target_backup = backup/path.relative_to(root)
        target_backup.parent.mkdir(parents=True, exist_ok=True)
        if not target_backup.exists():
            shutil.copy2(path, target_backup)
        tmp = path.with_suffix('.emotion-tmp')
        tmp.write_text(''.join(updated), encoding='utf-8')
        tmp.replace(path)
    print(json.dumps({'grammars_updated': len(paths)}))


def analyze(project_file: Path, word: Path, output: Path, alias_file: Path | None = None) -> None:
    """Write all exact matches, ambiguous candidates and unmatched interventions."""
    project = read_json(project_file)
    script = read_json(project_file.parent / 'script/narration-script.json')
    aliases = {}
    for char in project['theatre']['characters']:
        for name in [char['id'], char['displayName'], *char.get('aliases', [])]:
            aliases[normalized(name)] = char['id']
    if alias_file:
        aliases.update({normalized(name): char_id for name, char_id in read_json(alias_file).items()})
    paras = paragraphs(word)
    speeches = []
    for index, paragraph in enumerate(paras):
        if ':' not in paragraph:
            continue
        speaker, text = paragraph.split(':', 1)
        char_id = aliases.get(normalized(speaker)) or aliases.get(normalized(speaker.split(',')[0].split('/')[0]))
        if char_id:
            speeches.append({'paragraph': index + 1, 'characterId': char_id,
                             'speaker': speaker, 'text': text.strip(),
                             'before': paras[max(0, index-2):index],
                             'after': paras[index+1:index+3]})
    exact = defaultdict(list)
    by_character = defaultdict(list)
    for speech in speeches:
        exact[(speech['characterId'], normalized(speech['text']))].append(speech)
        by_character[speech['characterId']].append(speech)
    rows = []
    for segment in script['segments']:
        meta = segment.get('metadata', {})
        intervention = meta.get('theatreGrammarInterventionId')
        if not intervention:
            continue
        char_id = segment['characterId']
        key = normalized(segment['narrationText'])
        matches = exact.get((char_id, key), [])
        candidates = []
        if not matches:
            ranked = sorted(((difflib.SequenceMatcher(None, key, normalized(s['text']), autojunk=False).ratio(), s)
                             for s in by_character[char_id]), key=lambda pair: pair[0], reverse=True)
            candidates = [{'similarity': round(score, 4), **s} for score, s in ranked[:2]]
        rows.append({'intervention': intervention, 'segment': segment['id'],
                     'characterId': char_id, 'scene': meta.get('sceneName'),
                     'text': segment['narrationText'], 'matches': matches, 'candidates': candidates})
    output.mkdir(parents=True, exist_ok=True)
    (output/'cross.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
    (output/'word-paragraphs.json').write_text(json.dumps(paras, ensure_ascii=False, indent=2), encoding='utf-8')
    counts = Counter('exact' if len(r['matches']) == 1 else 'repeated' if r['matches'] else 'unmatched' for r in rows)
    print(json.dumps({'paragraphs': len(paras), 'human_speeches': len(speeches), 'project': len(rows), 'matches': counts}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('project', type=Path)
    parser.add_argument('word', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--aliases', type=Path)
    parser.add_argument('--rules', type=Path)
    parser.add_argument('--runtime', type=Path, default=Path.cwd())
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--sync-backup', type=Path)
    args = parser.parse_args()
    if args.sync_backup:
        sync_grammar(args.project, args.output, args.sync_backup)
    elif args.apply:
        apply_decisions(args.project, args.output)
    else:
        analyze(args.project, args.word, args.output, args.aliases)
        if args.rules:
            prepare(args.project, args.word, args.output, args.rules, args.runtime)
