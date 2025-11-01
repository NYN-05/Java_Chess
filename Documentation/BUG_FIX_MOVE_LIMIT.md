# Bug Fix: Chess Pieces Stop Moving After 100+ Moves

## Issue Description
**Bug**: Chess pieces stopped moving after a certain number of steps (approximately 100 moves).

## Root Cause
The bug was in the `saveState()` method that manages the undo history. When the undo stack exceeded `MAX_HISTORY` (100 moves), the code attempted to remove old states using an inefficient and incorrect approach:

### Problematic Code:
```java
if (undoStack.size() > MAX_HISTORY) {
    // Remove the oldest state
    GameState[] temp = undoStack.toArray(new GameState[0]);
    undoStack.clear();
    for (int i = 1; i < temp.length; i++) {
        undoStack.addLast(temp[i]);  // ❌ BUG: Wrong method for stack!
    }
}
```

### The Problem:
1. `undoStack` is a `Deque` used as a stack
2. `push()` adds to the **front** of the deque
3. `addLast()` adds to the **back** of the deque
4. Mixing these operations corrupted the stack order
5. After 100 moves, the entire history was converted to an array, cleared, and re-added in the wrong order
6. This caused move validation to fail, making pieces appear immovable

## Solution
Replaced the complex array conversion with a simple, efficient operation:

```java
if (undoStack.size() > MAX_HISTORY) {
    // Remove the oldest state (at the bottom of the stack)
    undoStack.removeLast();  // ✅ FIXED: Proper stack operation
}
```

### Why This Works:
1. `push()` adds to the **front** (top of stack)
2. `removeLast()` removes from the **back** (bottom of stack)
3. This maintains proper LIFO (Last In, First Out) order
4. Oldest states are correctly removed
5. Stack integrity is preserved

## Impact
- ✅ **Before**: Pieces stopped moving after ~100 moves
- ✅ **After**: Pieces move correctly for unlimited moves
- ✅ **Performance**: Much faster (O(1) vs O(n) operation)
- ✅ **Memory**: Still limited to 100 states as intended

## Testing
1. Compile: `javac -d bin Chess.java` ✅
2. Run: `java -cp bin Chess` ✅
3. Make 100+ moves - pieces continue to work normally ✅

## Technical Details

### Deque Stack Operations
When using `Deque` as a stack:
- `push(e)` = `addFirst(e)` - Add to front (top)
- `pop()` = `removeFirst()` - Remove from front (top)
- `peek()` = `peekFirst()` - Look at front (top)
- `removeLast()` - Remove from back (bottom) - Perfect for removing oldest!

### History Management
```
After 100 moves:
┌─────────────────────┐
│ Move 100 (newest)   │ ← push() adds here
│ Move 99             │
│ Move 98             │
│ ...                 │
│ Move 2              │
│ Move 1 (oldest)     │ ← removeLast() removes here
└─────────────────────┘
```

## Files Changed
- `Chess.java` - Fixed `saveState()` method (line ~540)

## Status
✅ **FIXED** - Chess pieces now move correctly for unlimited moves!

---
*Bug Fixed: November 1, 2025*
*Impact: Critical bug affecting gameplay after 100 moves*
*Severity: High*
*Fix Complexity: Low (3 lines changed)*
