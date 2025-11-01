# 🔧 Customization & Extension Guide

## Quick Customization Tips

### 1. Change Board Colors

Find these lines in `ChessGUI` class (around line 488):
```java
private static final java.awt.Color LIGHT_SQUARE = java.awt.Color.decode("#F0D9B5");
private static final java.awt.Color DARK_SQUARE = java.awt.Color.decode("#B58863");
```

**Popular Color Schemes:**

**Classic Green:**
```java
LIGHT_SQUARE = java.awt.Color.decode("#EEEED2");
DARK_SQUARE = java.awt.Color.decode("#769656");
```

**Blue Theme:**
```java
LIGHT_SQUARE = java.awt.Color.decode("#DEE3E6");
DARK_SQUARE = java.awt.Color.decode("#8CA2AD");
```

**Modern Gray:**
```java
LIGHT_SQUARE = java.awt.Color.decode("#E8E8E8");
DARK_SQUARE = java.awt.Color.decode("#4A4A4A");
```

**Marble:**
```java
LIGHT_SQUARE = java.awt.Color.decode("#F0F0F0");
DARK_SQUARE = java.awt.Color.decode("#8B7355");
```

### 2. Adjust UI Panel Theme

Around line 494-496:
```java
private static final java.awt.Color PANEL_BG = java.awt.Color.decode("#2C2C2C");
private static final java.awt.Color PANEL_FG = java.awt.Color.decode("#E8E8E8");
private static final java.awt.Color ACCENT_COLOR = java.awt.Color.decode("#4A9EFF");
```

**Light Theme Alternative:**
```java
PANEL_BG = java.awt.Color.decode("#F5F5F5");
PANEL_FG = java.awt.Color.decode("#333333");
ACCENT_COLOR = java.awt.Color.decode("#2196F3");
```

### 3. Change Highlight Colors

Around line 490-493:
```java
private static final java.awt.Color SELECTED_HIGHLIGHT = new java.awt.Color(255, 255, 102, 180);
private static final java.awt.Color LEGAL_MOVE_HIGHLIGHT = new java.awt.Color(124, 252, 0, 100);
private static final java.awt.Color CAPTURE_HIGHLIGHT = new java.awt.Color(255, 99, 71, 120);
private static final java.awt.Color LAST_MOVE_HIGHLIGHT = new java.awt.Color(205, 210, 106, 120);
```

**Subtle Blue Theme:**
```java
SELECTED_HIGHLIGHT = new java.awt.Color(100, 150, 255, 150);
LEGAL_MOVE_HIGHLIGHT = new java.awt.Color(100, 180, 255, 100);
CAPTURE_HIGHLIGHT = new java.awt.Color(255, 150, 100, 120);
LAST_MOVE_HIGHLIGHT = new java.awt.Color(150, 180, 255, 100);
```

### 4. Modify Window Size

In `createAndShowGUI()` method (around line 540):
```java
boardPanel.setPreferredSize(new Dimension(640, 640));
frame.setMinimumSize(new Dimension(900, 700));
```

**Larger Window:**
```java
boardPanel.setPreferredSize(new Dimension(800, 800));
frame.setMinimumSize(new Dimension(1100, 900));
```

### 5. Change Fonts

Look for font definitions:
```java
Font("Segoe UI", Font.BOLD, 28)     // Title
Font("Segoe UI", Font.BOLD, 16)     // Headers
Font("Segoe UI", Font.BOLD, 14)     // Buttons
Font("Courier New", Font.PLAIN, 13) // Move history
```

**Alternative Modern Fonts:**
```java
Font("Arial", Font.BOLD, 28)
Font("Helvetica", Font.BOLD, 16)
Font("Consolas", Font.PLAIN, 13)
```

## Advanced Customizations

### Add Sound Effects

**Step 1:** Add sound support:
```java
import javax.sound.sampled.*;
import java.io.File;

private void playSound(String soundFile) {
    try {
        AudioInputStream audio = AudioSystem.getAudioInputStream(new File(soundFile));
        Clip clip = AudioSystem.getClip();
        clip.open(audio);
        clip.start();
    } catch (Exception e) {
        // Handle error
    }
}
```

**Step 2:** Call in mouse handler after successful move:
```java
if (ok) {
    playSound("sounds/move.wav");
    // ... existing code
}
```

### Add Piece Animation

**Step 1:** Add animation timer:
```java
private Timer animationTimer;
private Pos animateFrom, animateTo;
private double animationProgress = 0;
```

**Step 2:** Create animation method:
```java
private void animateMove(Move move) {
    animateFrom = move.from;
    animateTo = move.to;
    animationProgress = 0;
    
    animationTimer = new Timer(10, e -> {
        animationProgress += 0.05;
        if (animationProgress >= 1.0) {
            animationTimer.stop();
            animationProgress = 0;
        }
        repaint();
    });
    animationTimer.start();
}
```

### Add Move Time Tracking

**Step 1:** Add fields:
```java
private long moveStartTime;
private List<Long> moveTimes = new ArrayList<>();
```

**Step 2:** Track times:
```java
// When turn changes:
if (moveStartTime > 0) {
    moveTimes.add(System.currentTimeMillis() - moveStartTime);
}
moveStartTime = System.currentTimeMillis();
```

**Step 3:** Display in panel:
```java
JLabel timeLabel = new JLabel("Avg time: " + getAverageTime() + "s");
```

### Add Chess Clock

**Step 1:** Add timer fields:
```java
private int whiteTimeSeconds = 600; // 10 minutes
private int blackTimeSeconds = 600;
private Timer clockTimer;
```

**Step 2:** Create clock update method:
```java
private void startClock() {
    clockTimer = new Timer(1000, e -> {
        if (game.getTurn() == Color.WHITE) {
            whiteTimeSeconds--;
        } else {
            blackTimeSeconds--;
        }
        updateClockDisplay();
        if (whiteTimeSeconds <= 0 || blackTimeSeconds <= 0) {
            clockTimer.stop();
            handleTimeOut();
        }
    });
    clockTimer.start();
}
```

### Add Board Themes

**Step 1:** Create theme class:
```java
class BoardTheme {
    String name;
    Color lightSquare, darkSquare;
    
    BoardTheme(String name, String light, String dark) {
        this.name = name;
        this.lightSquare = Color.decode(light);
        this.darkSquare = Color.decode(dark);
    }
}
```

**Step 2:** Define themes:
```java
private BoardTheme[] themes = {
    new BoardTheme("Classic", "#F0D9B5", "#B58863"),
    new BoardTheme("Green", "#EEEED2", "#769656"),
    new BoardTheme("Blue", "#DEE3E6", "#8CA2AD"),
    new BoardTheme("Gray", "#E8E8E8", "#4A4A4A")
};
```

**Step 3:** Add theme selector to menu:
```java
JMenu themeMenu = new JMenu("Themes");
for (BoardTheme theme : themes) {
    JMenuItem item = new JMenuItem(theme.name);
    item.addActionListener(e -> applyTheme(theme));
    themeMenu.add(item);
}
menuBar.add(themeMenu);
```

### Add Move Hints

**Step 1:** Add hint button:
```java
JButton hintBtn = createStyledButton("Show Hint");
hintBtn.addActionListener(e -> showHint());
```

**Step 2:** Implement hint logic:
```java
private void showHint() {
    List<Move> legal = game.getLegalMovesForTurn();
    if (!legal.isEmpty()) {
        Move hint = legal.get(0); // or use basic evaluation
        boardPanel.highlightHint(hint);
    }
}
```

### Save/Load Games (PGN Format)

**Step 1:** Add menu items:
```java
JMenuItem saveItem = new JMenuItem("Save Game");
saveItem.addActionListener(e -> saveGame());
JMenuItem loadItem = new JMenuItem("Load Game");
loadItem.addActionListener(e -> loadGame());
```

**Step 2:** Implement save:
```java
private void saveGame() {
    JFileChooser chooser = new JFileChooser();
    if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
        try (PrintWriter out = new PrintWriter(chooser.getSelectedFile())) {
            for (int i = 0; i < movesListModel.size(); i++) {
                out.println(movesListModel.get(i));
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving game");
        }
    }
}
```

## Testing Your Changes

After making changes:

1. **Recompile:**
   ```bash
   javac Chess.java
   ```

2. **Run:**
   ```bash
   java Chess
   ```

3. **Check:**
   - Window opens correctly
   - Colors look good
   - No exceptions in console
   - All features still work

## Common Issues & Solutions

**Issue:** Colors don't change
- **Solution:** Make sure you're using the constant names, not hardcoded values

**Issue:** Layout breaks on resize
- **Solution:** Check that you didn't modify `computeBoardGeometry()`

**Issue:** Pieces don't scale
- **Solution:** Verify image cache logic hasn't been modified

**Issue:** Highlights don't show
- **Solution:** Check alpha channel in Color constructor (4th parameter)

## Performance Tips

1. **Don't redraw unnecessarily:** Only call `repaint()` when needed
2. **Cache images properly:** Use the existing cache system
3. **Avoid heavy computation in paint:** Pre-calculate in mouse handlers
4. **Use timers wisely:** Stop timers when not needed

## Contributing Ideas

Want to add more features? Consider:
- [ ] Undo/Redo with move stack
- [ ] Opening book database
- [ ] Position evaluation display
- [ ] Move suggestions for beginners
- [ ] Puzzle mode
- [ ] Multiple board themes selector
- [ ] Dark mode toggle
- [ ] Export to PNG/PDF
- [ ] Online play with sockets
- [ ] AI opponent (minimax + alpha-beta)

---

**Happy customizing! Make it your own! 🎨**
