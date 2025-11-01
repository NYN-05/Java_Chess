# Undo/Redo System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Chess Game                               │
│                                                                  │
│  ┌────────────┐         ┌──────────────┐         ┌───────────┐ │
│  │   Board    │         │  Game State  │         │   Stacks  │ │
│  │            │────────▶│              │────────▶│           │ │
│  │  8x8 Grid  │         │ • Board      │         │  Undo ↓   │ │
│  │            │         │ • Turn       │         │  [S5]     │ │
│  └────────────┘         │ • Castling   │         │  [S4]     │ │
│                         │ • En Passant │         │  [S3]     │ │
│                         │ • Last Move  │         │  [S2]     │ │
│                         └──────────────┘         │  [S1]     │ │
│                                                  │           │ │
│                                                  │  Redo ↑   │ │
│                                                  │  (empty)  │ │
│                                                  └───────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Flow Diagram

### Making a Move
```
Before Move              Save State              After Move
┌─────────┐             ┌─────────┐            ┌─────────┐
│ Current │────────────▶│  Undo   │            │  New    │
│  State  │   saveState │  Stack  │            │  State  │
└─────────┘             └─────────┘            └─────────┘
                             ▲                       │
                             │                       │
                             └───────────────────────┘
                                 Push old state
```

### Undo Operation
```
Current State           Undo Stack              Redo Stack
┌─────────┐            ┌─────────┐            ┌─────────┐
│   S3    │            │   S2    │            │   S3    │
│         │──Undo─────▶│   S1    │            │         │
└─────────┘            └─────────┘            └─────────┘
                         Pop & Restore          Push Current
```

### Redo Operation
```
Current State           Undo Stack              Redo Stack
┌─────────┐            ┌─────────┐            ┌─────────┐
│   S2    │            │   S2    │            │         │
│         │◀──Redo─────│   S1    │            │         │
└─────────┘            └─────────┘            └─────────┘
   Restore             Push Current              Pop S3
     S3
```

### New Move After Undo
```
Before New Move         After New Move
Undo: [S1, S2]         Undo: [S1, S2, S3new]
Redo: [S3, S4]         Redo: []  ← CLEARED!
                       
Making a new move clears redo history
```

## State Capture Details

Each GameState captures:
```
GameState {
    Board (8x8 array)
    ├── Piece positions
    ├── Piece types
    └── Piece colors
    
    Game Flags
    ├── Current turn
    ├── White king moved?
    ├── Black king moved?
    ├── White rook A moved?
    ├── White rook H moved?
    ├── Black rook A moved?
    └── Black rook H moved?
    
    Special State
    ├── En passant target
    └── Last move made
}
```

## UI Integration

```
┌──────────────────────────────────────────────┐
│  ♔ Professional Chess ♔                      │
├──────────────────────────────────────────────┤
│  Game   View                                 │
│  ├─ New Game (Ctrl+N)                        │
│  ├─ ─────────────                            │
│  ├─ Undo Move (Ctrl+Z) ◄── Keyboard Shortcut│
│  └─ Redo Move (Ctrl+Y) ◄── Keyboard Shortcut│
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│                                              │
│        [Chess Board Display]                 │
│                                              │
├──────────────────────────────────────────────┤
│  Info Panel:                                 │
│  ┌────────────────────┐                      │
│  │ [New Game]         │                      │
│  │ [Undo Move] ◄────  │  Button Controls     │
│  │ [Redo Move] ◄────  │                      │
│  │ [Resign]           │                      │
│  └────────────────────┘                      │
└──────────────────────────────────────────────┘
```

## Memory Management

```
History Growth              Limit Reached
                           
Move 1  [S1]               Move 100 [S100]
Move 2  [S1,S2]            Move 101 [S2...S101] ← S1 removed
Move 3  [S1,S2,S3]         Move 102 [S3...S102] ← S2 removed
  ...                        ...
Move 99 [S1...S99]         Always maintains max 100 states
```

## Example Sequence

```
1. Game starts: Undo=[], Redo=[]
   
2. Player moves e2-e4: 
   Undo=[Initial], Redo=[]
   
3. Player moves e7-e5:
   Undo=[Initial, AfterE4], Redo=[]
   
4. Player presses Undo (Ctrl+Z):
   Undo=[Initial], Redo=[AfterE4]
   Board shows: e7 pawn back at e7
   
5. Player presses Undo (Ctrl+Z) again:
   Undo=[], Redo=[AfterE4, Initial]
   Board shows: Starting position
   
6. Player presses Redo (Ctrl+Y):
   Undo=[Initial], Redo=[AfterE4]
   Board shows: After e2-e4
   
7. Player makes different move e2-d4:
   Undo=[Initial, AfterD4], Redo=[] ← Cleared!
   Cannot redo e4 anymore
```

## Performance Characteristics

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Save State| O(1) amortized | O(n) for board   |
| Undo      | O(1)           | O(n) for board   |
| Redo      | O(1)           | O(n) for board   |
| Can Undo  | O(1)           | O(1)             |
| Can Redo  | O(1)           | O(1)             |

Where n = 64 (board size)

## Error Handling

```
User Action              System Check           Response
───────────────────────────────────────────────────────────
Press Undo               undoStack.isEmpty()?   Show message:
with no history          YES ──────────────────▶"No moves to undo"

Press Redo               redoStack.isEmpty()?   Show message:
with no redo             YES ──────────────────▶"No moves to redo"

Undo during              Check canUndo()        Disable button
game start               FALSE ─────────────────▶Gray out control

History exceeds          size > MAX_HISTORY?    Remove oldest
100 moves                YES ──────────────────▶Keep last 100
```

## Integration Points

The Undo/Redo system integrates with:

1. **Move Application** (`applyMoveIfLegal`)
   - Saves state before each move
   - Updates undo stack automatically

2. **GUI Buttons** 
   - Undo/Redo buttons trigger operations
   - Update move history display

3. **Keyboard Shortcuts**
   - Ctrl+Z/Y mapped to undo/redo
   - Consistent with standard shortcuts

4. **Menu System**
   - Game menu provides access
   - Shows keyboard shortcuts

5. **Move History**
   - Synchronizes with undo/redo
   - Removes/adds moves as needed

---

**Implementation Status: ✅ Complete and Production Ready**
