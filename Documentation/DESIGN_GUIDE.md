# 🎨 Visual Design Guide

## Color Palette

### Board Colors
- **Light Squares**: #F0D9B5 (Warm cream)
- **Dark Squares**: #B58863 (Rich brown)
- **Board Border**: #505050 (Dark gray)

### Highlight Colors
- **Selection**: rgba(255, 255, 102, 180) - Bright yellow with transparency
- **Legal Move**: rgba(124, 252, 0, 100) - Green with transparency
- **Capture**: rgba(255, 99, 71, 120) - Red with transparency
- **Last Move**: rgba(205, 210, 106, 120) - Yellow-green

### UI Panel Colors
- **Background**: #2C2C2C (Dark charcoal)
- **Text**: #E8E8E8 (Light gray)
- **Accent**: #4A9EFF (Professional blue)
- **List Background**: #1E1E1E (Very dark gray)

## Typography

### Fonts Used
1. **Segoe UI** (UI elements)
   - Bold 28pt: Title
   - Bold 16pt: Section headers
   - Bold 14pt: Buttons
   - Plain 14pt: Labels
   
2. **Segoe UI Symbol** (Chess pieces)
   - Size: 70% of cell size
   
3. **Courier New** (Move history)
   - Plain 13pt: Monospaced for alignment

### Chess Unicode Symbols
- White: ♔ ♕ ♖ ♗ ♘ ♙
- Black: ♚ ♛ ♜ ♝ ♞ ♟

## Layout Structure

```
┌─────────────────────────────────────────────────────────┐
│  ♔ Professional Chess ♔                          [Menu] │
├────────────────────────────────┬────────────────────────┤
│                                │  ♔ CHESS ♔            │
│                                │                        │
│     8 ║ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░  │  ┌──────────────────┐ │
│     7 ║ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓  │  │ Current: ● White │ │
│     6 ║ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░  │  │ Move: 0          │ │
│     5 ║ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓  │  │ White to move    │ │
│     4 ║ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░  │  └──────────────────┘ │
│     3 ║ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓  │                        │
│     2 ║ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░  │  Move History         │
│     1 ║ ▓▓ ░░ ▓▓ ░░ ▓▓ ░░ ▓▓  │  ┌──────────────────┐ │
│       ╚═══════════════════════ │  │                  │ │
│         a  b  c  d  e  f  g  h │  │  (move list)     │ │
│                                │  │                  │ │
│                                │  └──────────────────┘ │
│                                │                        │
│                                │  [  New Game  ]        │
│                                │  [ Undo Move  ]        │
│                                │  [   Resign   ]        │
└────────────────────────────────┴────────────────────────┘
```

## Visual Effects

### 1. Piece Selection
When a piece is selected:
- Background: Bright yellow highlight
- Border: 3px yellow border
- Duration: Until move or deselection

### 2. Legal Moves
Regular moves:
- Small green circle (25% of cell size)
- Centered in square
- Slight transparency

Capture moves:
- Red border (4px thick)
- Rounded corners (10px radius)
- Fills entire square with transparency

### 3. Last Move
- Both 'from' and 'to' squares highlighted
- Yellow-green semi-transparent overlay
- Persists until next move

### 4. Piece Rendering
- Drop shadow: 2px offset, black 40% opacity
- Main piece: Full opacity
- Outline: For white pieces, subtle gray

### 5. Board Depth
- Inner shadow on squares: 1px, black 15% opacity
- Board border: 4px dark gray frame
- Creates "sunken" effect

## Button States

### Normal State
- Background: #4A9EFF (blue)
- Text: White, bold
- Border: None (flat design)
- Size: 250x40 pixels

### Hover State
- Background: Brighter blue
- Cursor: Hand pointer
- Transition: Smooth color change

### Disabled State
- Background: Gray
- Text: Light gray
- No hover effect

## Panel Layout

### Info Panel Spacing
- Width: 280px fixed
- Padding: 15px all sides
- Gap between sections: 20px
- Border: 2px accent color

### Section Boxes
- Background: Panel background
- Border: 2px accent color
- Padding: 10px
- Rounded corners: Optional

## Responsive Behavior

### Window Resize
- Board: Maintains square aspect ratio
- Minimum board: 400x400 pixels
- Maximum board: Fills available space
- Panel: Fixed width, scrolls if needed

### Cell Size Calculation
```
Available space = min(width - 30, height - 30)
Cell size = floor(available space / 8)
Board size = cell size × 8
Center offset = (total - board) / 2
```

## Accessibility Features

### High Contrast Mode
- Light squares: Pure white (#FFFFFF)
- Dark squares: Pure black (#000000)
- Removes all transparency
- Higher color contrast for visually impaired

### Keyboard Navigation
- Visual cursor indicator
- Selected square highlighted
- Arrow key movement
- Enter to confirm

## Professional Polish Details

### Shadows
1. **Piece shadows**: 2px offset, 40% black
2. **Text shadows**: 1px offset on coordinates
3. **Board border**: Subtle gradient effect

### Anti-Aliasing
- All shapes: Anti-aliased
- Text: Sub-pixel rendering
- Images: Bicubic interpolation

### Spacing
- Consistent 10-20px gaps
- Aligned elements
- Visual hierarchy clear

### Feedback
- Instant visual response to clicks
- Smooth hover transitions
- Clear state indicators

---

This design creates a premium, professional chess application that's both beautiful and functional!
