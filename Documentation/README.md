# Simple Java Chess (Console)

This is a small single-file Java console chess implementation with basic rules:

- Move input: `e2 e4` or `e2e4` (also `e7e8q` for promotion to queen)
- Implements: legal move generation, check detection, checkmate/stalemate detection, pawn promotion to queen
- Not implemented: castling, en passant, draw by repetition, 50-move rule, AI

How to compile and run (PowerShell):

```powershell
javac Chess.java
java Chess
```

Controls: type moves and press Enter. Type `quit` or `exit` to leave.

Next steps / improvements:
- Add castling, en passant
- Add move history and undo
- Add simple AI (minimax)
- Provide GUI (Swing/JavaFX) or web UI
