#Requires -Version 5.1
param(
    [string]$SourcePath = 'C:\Users\ehdrl\Documents\이직\포트폴리오_정동길.pptx',
    [string]$BackupPath = 'C:\Users\ehdrl\Documents\이직\포트폴리오_정동길_backup.pptx',
    [string]$WorkPath = 'C:\Users\ehdrl\rag-doc-platform\backend\deploy\scripts\portfolio_work.pptx'
)

$ErrorActionPreference = 'Stop'

function RGB([int]$r, [int]$g, [int]$b) { return $r + ($g * 256) + ($b * 65536) }

$C = @{
    Teal       = (RGB 13 148 136)
    TealBright = (RGB 45 212 190)
    Dark       = (RGB 15 23 42)
    Gray       = (RGB 71 85 104)
    GrayLight  = (RGB 100 116 139)
    Blue       = (RGB 79 129 189)
    BgLight    = (RGB 241 244 249)
    White      = (RGB 255 255 255)
    Border     = (RGB 225 231 240)
}

function Add-Rect($slide, $left, $top, $width, $height, $fill, $line = $null) {
    $s = $slide.Shapes.AddShape(1, $left, $top, $width, $height)
    $s.Fill.ForeColor.RGB = $fill
    if ($line) { $s.Line.ForeColor.RGB = $line; $s.Line.Weight = 1 } else { $s.Line.Visible = 0 }
    return $s
}

function Add-RoundRect($slide, $left, $top, $width, $height, $fill, $text, $fontSize = 10, $bold = $false, $fontColor = $null) {
    $s = $slide.Shapes.AddShape(5, $left, $top, $width, $height)
    $s.Fill.ForeColor.RGB = $fill
    $s.Line.ForeColor.RGB = $C.Border
    $s.Line.Weight = 0.75
    $s.TextFrame.TextRange.Text = $text
    $s.TextFrame.TextRange.Font.Name = 'Malgun Gothic'
    $s.TextFrame.TextRange.Font.Size = $fontSize
    $s.TextFrame.TextRange.Font.Bold = $bold
    $s.TextFrame.TextRange.Font.Color.RGB = $(if ($fontColor) { $fontColor } else { $C.Dark })
    $s.TextFrame.VerticalAnchor = 3
    $s.TextFrame.TextRange.ParagraphFormat.Alignment = 2
    return $s
}

function Add-Text($slide, $left, $top, $width, $height, $text, $size, $bold = $false, $color = $null, $align = 1) {
    $tb = $slide.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $tb.Line.Visible = 0
    $tb.Fill.Visible = 0
    $r = $tb.TextFrame.TextRange
    $r.Text = $text
    $r.Font.Name = 'Malgun Gothic'
    $r.Font.Size = $size
    $r.Font.Bold = $bold
    $r.Font.Color.RGB = $(if ($color) { $color } else { $C.Dark })
    $r.ParagraphFormat.Alignment = $align
    return $tb
}

function Add-AccentBar($slide, $left, $top, $height) {
    Add-Rect $slide $left $top 6 $height $C.Teal | Out-Null
}

function Add-SectionLabel($slide, $left, $top, $text) {
    $tb = Add-Text $slide $left $top 200 18 $text 9 $false $C.GrayLight 1
    $tb.TextFrame.TextRange.Font.Name = 'Arial Black'
    return $tb
}

function Add-Arrow($slide, $x1, $y1, $x2, $y2) {
    $s = $slide.Shapes.AddConnector(1, $x1, $y1, $x2, $y2)
    $s.Line.ForeColor.RGB = $C.Teal
    $s.Line.Weight = 2
    $s.Line.EndArrowheadStyle = 2
    return $s
}

if (-not (Test-Path $SourcePath)) { throw "File not found: $SourcePath" }
Copy-Item $SourcePath $BackupPath -Force
Copy-Item $SourcePath $WorkPath -Force

$pp = New-Object -ComObject PowerPoint.Application
$pp.Visible = [Microsoft.Office.Core.MsoTriState]::msoTrue
$pres = $pp.Presentations.Open($WorkPath, $false, $false, $false)

# Slide A: Appendix cover
$sa = $pres.Slides.Add($pres.Slides.Count + 1, 12)
Add-Rect $sa 0 0 960 540 (RGB 248 250 252) | Out-Null
Add-Rect $sa 0 0 960 4 $C.Teal | Out-Null
Add-Text $sa 80 180 800 30 'APPENDIX' 14 $false $C.Teal 1 | Out-Null
Add-Text $sa 80 215 800 55 '개인 학습 프로젝트' 36 $true $C.Dark 1 | Out-Null
Add-Text $sa 80 280 800 40 'RAG Doc Platform' 28 $true $C.TealBright 1 | Out-Null
Add-Text $sa 80 330 800 60 "PDF 문서 기반 RAG(Retrieval-Augmented Generation) 챗봇 플랫폼`n풀스택 개발 / 하이브리드 검색 / Docker CI/CD" 14 $false $C.Gray 1 | Out-Null
Add-Rect $sa 80 400 120 3 $C.Teal | Out-Null
Add-Text $sa 80 415 400 20 'Personal Side Project / 2025-2026' 11 $false $C.GrayLight 1 | Out-Null

# Slide B: Project overview
$sb = $pres.Slides.Add($pres.Slides.Count + 1, 12)
Add-AccentBar $sb 36 40 460 | Out-Null
Add-Text $sb 52 38 620 36 '별첨 1. RAG Doc Platform - PDF RAG 챗봇' 22 $true $C.Dark 1 | Out-Null
Add-Rect $sb 36 88 420 72 $C.BgLight $C.Border | Out-Null
Add-Rect $sb 36 88 100 72 $C.BgLight $C.Border | Out-Null
Add-Text $sb 44 108 90 20 '프로젝트' 10 $false $C.Dark 1 | Out-Null
Add-Text $sb 44 122 90 20 '유형' 10 $false $C.Dark 1 | Out-Null
Add-Rect $sb 136 88 320 36 $C.Blue | Out-Null
Add-Text $sb 144 98 300 22 '개인 학습 (Full-Stack / RAG)' 11 $true $C.White 1 | Out-Null
Add-Rect $sb 136 124 100 36 $C.BgLight $C.Border | Out-Null
Add-Text $sb 144 134 90 20 '기간' 10 $false $C.Dark 1 | Out-Null
Add-Text $sb 240 134 200 20 '2025 ~ 2026 (개인)' 10 $false $C.Dark 1 | Out-Null
Add-Text $sb 52 175 400 24 '프로젝트 개요' 16 $true $C.Dark 1 | Out-Null
$bullets = @(
    '목적: 업로드한 PDF를 기반으로 검색 및 질의응답하는 RAG 서비스 구현',
    '역할: 기획 / 백엔드 / 프론트엔드 / DB / DevOps 전 과정 1인 개발',
    '범위: JWT 인증, PDF 수집/청킹, 벡터 검색, LLM 채팅, Docker 배포',
    '특징: Parent-Child 청킹 + Hybrid Search + RRF + LLM Rerank 파이프라인'
)
$y = 205
foreach ($b in $bullets) {
    Add-Text $sb 52 $y 430 38 "- $b" 11 $false $C.Gray 1 | Out-Null
    $y += 42
}
Add-Rect $sb 520 80 400 200 (RGB 15 23 42) | Out-Null
Add-SectionLabel $sb 535 95 'PROJECT HIGHLIGHT' | Out-Null
Add-Text $sb 535 118 370 40 'Hybrid RAG' 28 $true $C.TealBright 1 | Out-Null
Add-Text $sb 535 162 370 50 'Dense + Keyword -> RRF -> Rerank -> Parent Expand' 10 $false (RGB 203 213 225) 1 | Out-Null
Add-Text $sb 535 220 370 40 'End-to-End: PDF 업로드 -> 임베딩 -> 채팅 UI -> CI/CD 배포' 10 $false (RGB 203 213 225) 1 | Out-Null
Add-Text $sb 36 395 200 20 'Tech Stack' 12 $true $C.Dark 1 | Out-Null
$badges = @('Java 21', 'Spring Boot 3.5', 'React 19', 'PostgreSQL', 'pgvector', 'Redis', 'Kafka', 'Docker', 'GitHub Actions')
$bx = 36; $by = 420
foreach ($badge in $badges) {
    $w = [math]::Max(72, $badge.Length * 7 + 16)
    if ($bx + $w -gt 480) { $bx = 36; $by += 32 }
    Add-RoundRect $sb $bx $by $w 26 $C.White $badge 9 $false $C.Gray | Out-Null
    $bx += $w + 8
}

# Slide C: RAG pipeline
$sc = $pres.Slides.Add($pres.Slides.Count + 1, 12)
Add-AccentBar $sc 36 40 460 | Out-Null
Add-Text $sc 52 38 700 36 '별첨 2. RAG 파이프라인 아키텍처' 22 $true $C.Dark 1 | Out-Null
Add-Text $sc 52 95 200 20 'Ingestion' 12 $true $C.Teal 1 | Out-Null
$steps = @('PDF Upload', 'Section Split', 'Child Chunk', 'Embedding', 'pgvector')
$sx = 52; $sy = 125
for ($i = 0; $i -lt $steps.Count; $i++) {
    Add-RoundRect $sc $sx $sy 100 44 $C.BgLight $steps[$i] 9 $false $C.Dark | Out-Null
    if ($i -lt $steps.Count - 1) { Add-Arrow $sc ($sx + 102) ($sy + 22) ($sx + 118) ($sy + 22) | Out-Null }
    $sx += 120
}
Add-Text $sc 52 195 200 20 'Retrieval' 12 $true $C.Teal 1 | Out-Null
$retSteps = @('Query', 'Dense Search', 'Keyword FTS', 'RRF Merge', 'LLM Rerank', 'Parent Expand', 'LLM Answer')
$sx = 52; $sy = 225
for ($i = 0; $i -lt $retSteps.Count; $i++) {
    $w = if ($retSteps[$i].Length -gt 8) { 88 } else { 72 }
    Add-RoundRect $sc $sx $sy $w 44 $C.BgLight $retSteps[$i] 8 $false $C.Dark | Out-Null
    if ($i -lt $retSteps.Count - 1) { Add-Arrow $sc ($sx + $w + 2) ($sy + 22) ($sx + $w + 14) ($sy + 22) | Out-Null }
    $sx += $w + 16
}
Add-Text $sc 52 295 200 20 'System Layers' 12 $true $C.Teal 1 | Out-Null
$layers = @(
    @{ Name = 'Frontend'; Sub = 'React 19 / Vite / JWT'; Color = (RGB 224 242 254) }
    @{ Name = 'Backend API'; Sub = 'Spring Boot 3.5 / REST'; Color = (RGB 204 251 241) }
    @{ Name = 'RAG Engine'; Sub = 'Chunking / Hybrid / Rerank'; Color = (RGB 167 243 208) }
    @{ Name = 'Data'; Sub = 'PostgreSQL / pgvector / Redis'; Color = (RGB 241 244 249) }
)
$ly = 325
foreach ($layer in $layers) {
    Add-RoundRect $sc 52 $ly 856 52 $layer.Color $layer.Name 12 $true $C.Dark | Out-Null
    Add-Text $sc 200 ($ly + 16) 680 24 $layer.Sub 10 $false $C.Gray 1 | Out-Null
    $ly += 58
}

# Slide D: CI/CD + DB
$sd = $pres.Slides.Add($pres.Slides.Count + 1, 12)
Add-AccentBar $sd 36 40 460 | Out-Null
Add-Text $sd 52 38 700 36 '별첨 3. 인프라 / CI/CD / 데이터 모델' 22 $true $C.Dark 1 | Out-Null
Add-Text $sd 52 95 200 20 'CI/CD Pipeline' 12 $true $C.Teal 1 | Out-Null
$cdSteps = @('Git Push', 'GitHub Actions', 'Gradle/npm Test', 'GHCR Push', 'Self-hosted Runner', 'Docker Deploy')
$sx = 52; $sy = 125
foreach ($step in $cdSteps) {
    $w = [math]::Max(90, $step.Length * 6 + 20)
    Add-RoundRect $sd $sx $sy $w 40 (RGB 204 251 241) $step 8 $false $C.Dark | Out-Null
    if ($step -ne $cdSteps[-1]) { Add-Arrow $sd ($sx + $w + 2) ($sy + 20) ($sx + $w + 14) ($sy + 20) | Out-Null }
    $sx += $w + 16
}
Add-Text $sd 52 195 200 20 'Database (Flyway V1-V6)' 12 $true $C.Teal 1 | Out-Null
Add-Rect $sd 52 220 420 200 $C.White $C.Border | Out-Null
$tables = @(
    'users - JWT 인증',
    'documents - PDF 메타/상태',
    'parent_sections - 섹션(Parent) 본문',
    'document_chunks - Child + vector(768)',
    'conversations / chat_messages - 채팅'
)
$ty = 232
foreach ($t in $tables) {
    Add-Text $sd 64 $ty 400 22 "> $t" 10 $false $C.Gray 1 | Out-Null
    $ty += 34
}
Add-Rect $sd 520 220 400 200 $C.BgLight $C.Border | Out-Null
Add-Text $sd 535 232 370 24 'DevOps 구성' 14 $true $C.Dark 1 | Out-Null
$devops = @(
    'Docker Compose: Backend :8081, Frontend :81',
    'Redis / Kafka 인프라 분리 운영',
    'GitHub Actions -> GHCR -> Self-hosted 배포',
    'Ollama 로컬 LLM / OpenAI Provider 전환',
    'Swagger API 문서 / Flyway 마이그레이션'
)
$dy = 262
foreach ($d in $devops) {
    Add-Text $sd 535 $dy 370 22 "- $d" 10 $false $C.Gray 1 | Out-Null
    $dy += 30
}
Add-Rect $sd 52 440 868 80 $C.BgLight $C.Border | Out-Null
Add-Text $sd 64 452 840 22 '학습 성과' 12 $true $C.Teal 1 | Out-Null
Add-Text $sd 64 478 840 32 'Production-grade RAG(Hybrid+RRF+Rerank) / Parent-Child 청킹 / Provider 패턴 / E2E 풀스택 + CI/CD' 11 $false $C.Gray 1 | Out-Null

$pres.Save()
$pres.Close()
$pp.Quit()

Copy-Item $WorkPath $SourcePath -Force
Remove-Item $WorkPath -Force -ErrorAction SilentlyContinue

Write-Host "Done. Backup: $BackupPath"
Write-Host "Updated: $SourcePath"
Write-Host "Total slides: 12"
