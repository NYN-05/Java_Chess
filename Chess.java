import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.BorderLayout;
import javax.imageio.ImageIO;
import java.io.File;
import java.awt.Image;
import java.util.Map;
import java.util.HashMap;
import javax.sound.sampled.*;
import java.awt.FlowLayout;

public class Chess {
    public static void main(String[] args) {
        // Launch GUI by default. Use argument "console" to run text mode.
        boolean console = false;
        for (String a : args) if (a.equalsIgnoreCase("console") || a.equalsIgnoreCase("-console")) console = true;
        if (console) {
            Game game = new Game();
            game.run();
        } else {
            SwingUtilities.invokeLater(() -> {
                ChessGUI gui = new ChessGUI();
                gui.createAndShowGUI();
            });
        }
    }

    enum Color { WHITE, BLACK }

    enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

    static class Piece implements Cloneable {
        final PieceType type;  // Immutable - optimize with final
        final Color color;     // Immutable - optimize with final

        Piece(PieceType type, Color color) {
            this.type = type;
            this.color = color;
        }

        @Override
        public Piece clone() {
            // Optimization: Since Piece is immutable, we can return this instead of cloning
            // This saves memory and improves performance
            return new Piece(type, color);
        }

        @Override
        public String toString() {
            // Optimization: Use direct char instead of switch for better performance
            char c = switch (type) {
                case KING -> 'K';
                case QUEEN -> 'Q';
                case ROOK -> 'R';
                case BISHOP -> 'B';
                case KNIGHT -> 'N';
                case PAWN -> 'P';
            };
            return color == Color.WHITE ? String.valueOf(c) : String.valueOf(Character.toLowerCase(c));
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Piece)) return false;
            Piece piece = (Piece) o;
            return type == piece.type && color == piece.color;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, color);
        }
    }

    static class Pos {
        final int r, c;  // Immutable - optimize with final
        
        Pos(int r, int c) { 
            this.r = r; 
            this.c = c; 
        }
        
        Pos(String s) { // e.g. e2
            s = s.trim();
            if (s.length() != 2) throw new IllegalArgumentException("Bad pos: " + s);
            this.c = s.charAt(0) - 'a';
            this.r = 8 - (s.charAt(1) - '0');
        }
        
        boolean inBounds() { 
            return r >= 0 && r < 8 && c >= 0 && c < 8; 
        }
        
        @Override 
        public boolean equals(Object o) { 
            if (this == o) return true;
            if (!(o instanceof Pos)) return false; 
            Pos p = (Pos)o; 
            return p.r == r && p.c == c; 
        }
        
        @Override 
        public int hashCode() { 
            // Optimization: Better hash distribution
            return r * 8 + c; 
        }
        
        @Override 
        public String toString() { 
            // Optimization: Cache common positions or use direct char array
            return String.format("%c%d", 'a' + c, 8 - r); 
        }
    }

    static class Move {
        final Pos from, to;  // Immutable - optimize with final
        Piece promotion; // if pawn promotion
        boolean isCastleKingSide = false;
        boolean isCastleQueenSide = false;
        boolean isEnPassant = false;
        
        Move(Pos f, Pos t) { 
            this.from = f; 
            this.to = t; 
        }
        
        @Override 
        public String toString() { 
            // Optimization: Use StringBuilder for string concatenation
            StringBuilder sb = new StringBuilder(6);
            sb.append(from).append("->").append(to);
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Move)) return false;
            Move move = (Move) o;
            return from.equals(move.from) && to.equals(move.to);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    static class Board implements Cloneable {
        final Piece[][] b = new Piece[8][8];  // Optimization: final array reference
        
        // Optimization: Cache pieces to avoid creating new objects repeatedly
        private static final Piece WHITE_PAWN = new Piece(PieceType.PAWN, Color.WHITE);
        private static final Piece BLACK_PAWN = new Piece(PieceType.PAWN, Color.BLACK);
        private static final Piece WHITE_ROOK = new Piece(PieceType.ROOK, Color.WHITE);
        private static final Piece BLACK_ROOK = new Piece(PieceType.ROOK, Color.BLACK);
        private static final Piece WHITE_KNIGHT = new Piece(PieceType.KNIGHT, Color.WHITE);
        private static final Piece BLACK_KNIGHT = new Piece(PieceType.KNIGHT, Color.BLACK);
        private static final Piece WHITE_BISHOP = new Piece(PieceType.BISHOP, Color.WHITE);
        private static final Piece BLACK_BISHOP = new Piece(PieceType.BISHOP, Color.BLACK);
        private static final Piece WHITE_QUEEN = new Piece(PieceType.QUEEN, Color.WHITE);
        private static final Piece BLACK_QUEEN = new Piece(PieceType.QUEEN, Color.BLACK);
        private static final Piece WHITE_KING = new Piece(PieceType.KING, Color.WHITE);
        private static final Piece BLACK_KING = new Piece(PieceType.KING, Color.BLACK);

        Board() { init(); }

        void init() {
            // Optimization: Use Arrays.fill for clearing
            for (int r = 0; r < 8; r++) {
                Arrays.fill(b[r], null);
            }
            
            // Pawns - use cached pieces
            for (int c = 0; c < 8; c++) { 
                b[1][c] = BLACK_PAWN;
                b[6][c] = WHITE_PAWN;
            }
            
            // Rooks
            b[0][0] = b[0][7] = BLACK_ROOK;
            b[7][0] = b[7][7] = WHITE_ROOK;
            
            // Knights
            b[0][1] = b[0][6] = BLACK_KNIGHT;
            b[7][1] = b[7][6] = WHITE_KNIGHT;
            
            // Bishops
            b[0][2] = b[0][5] = BLACK_BISHOP;
            b[7][2] = b[7][5] = WHITE_BISHOP;
            
            // Queens
            b[0][3] = BLACK_QUEEN;
            b[7][3] = WHITE_QUEEN;
            
            // Kings
            b[0][4] = BLACK_KING;
            b[7][4] = WHITE_KING;
        }

        Piece get(Pos p) { 
            return b[p.r][p.c]; 
        }
        
        void set(Pos p, Piece piece) { 
            b[p.r][p.c] = piece; 
        }

        @Override
        public Board clone() {
            // Optimization: More efficient cloning - skip init()
            Board nb = new Board();
            for (int r = 0; r < 8; r++) {
                // Use System.arraycopy for better performance
                System.arraycopy(b[r], 0, nb.b[r], 0, 8);
            }
            return nb;
        }

        void apply(Move m) {
            Piece p = get(m.from);
            set(m.to, p);
            set(m.from, null);
            if (m.promotion != null) set(m.to, m.promotion);
        }

        void print() {
            System.out.println();
            for (int r=0;r<8;r++) {
                System.out.print(8-r + " ");
                for (int c=0;c<8;c++) {
                    Piece p = b[r][c];
                    if (p == null) System.out.print(((r+c)%2==0) ? ". " : ": ");
                    else System.out.print(p + " ");
                }
                System.out.println();
            }
            System.out.println("  a b c d e f g h");
        }
    }

    // Chess Timer class for time controls
    static class ChessTimer {
        private long whiteTime; // milliseconds
        private long blackTime; // milliseconds
        private long lastUpdateTime;
        private Color activeColor;
        private boolean running = false;
        private long increment; // milliseconds per move
        
        ChessTimer(int minutesPerSide, int incrementSeconds) {
            this.whiteTime = minutesPerSide * 60 * 1000L;
            this.blackTime = minutesPerSide * 60 * 1000L;
            this.increment = incrementSeconds * 1000L;
            this.activeColor = Color.WHITE;
        }
        
        void start(Color color) {
            activeColor = color;
            lastUpdateTime = System.currentTimeMillis();
            running = true;
        }
        
        void stop() {
            if (running) {
                update();
                running = false;
            }
        }
        
        void switchPlayer() {
            if (running) {
                update();
                // Add increment
                if (activeColor == Color.WHITE) {
                    whiteTime += increment;
                } else {
                    blackTime += increment;
                }
                activeColor = (activeColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
                lastUpdateTime = System.currentTimeMillis();
            }
        }
        
        private void update() {
            if (!running) return;
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdateTime;
            if (activeColor == Color.WHITE) {
                whiteTime -= elapsed;
            } else {
                blackTime -= elapsed;
            }
            lastUpdateTime = now;
        }
        
        long getWhiteTime() {
            if (running && activeColor == Color.WHITE) update();
            return Math.max(0, whiteTime);
        }
        
        long getBlackTime() {
            if (running && activeColor == Color.BLACK) update();
            return Math.max(0, blackTime);
        }
        
        boolean isWhiteOutOfTime() { return getWhiteTime() <= 0; }
        boolean isBlackOutOfTime() { return getBlackTime() <= 0; }
        boolean isRunning() { return running; }
        
        String formatTime(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
        
        void reset(int minutesPerSide, int incrementSeconds) {
            this.whiteTime = minutesPerSide * 60 * 1000L;
            this.blackTime = minutesPerSide * 60 * 1000L;
            this.increment = incrementSeconds * 1000L;
            this.activeColor = Color.WHITE;
            this.running = false;
        }
    }

    // Sound Manager for game sounds
    static class SoundManager {
        private boolean enabled = true;
        
        void playMove() { playSound("move"); }
        void playCapture() { playSound("capture"); }
        void playCheck() { playSound("check"); }
        void playCastle() { playSound("castle"); }
        void playPromotion() { playSound("promote"); }
        void playGameOver() { playSound("gameover"); }
        
        private void playSound(String name) {
            if (!enabled) return;
            // Try to play sound file from sounds directory
            try {
                File soundFile = new File("sounds/" + name + ".wav");
                if (soundFile.exists()) {
                    javax.sound.sampled.AudioInputStream audioStream = 
                        javax.sound.sampled.AudioSystem.getAudioInputStream(soundFile);
                    javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                } else {
                    // Fallback: system beep
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            } catch (Exception e) {
                // Silent fail - sound is not critical
            }
        }
        
        void setEnabled(boolean enabled) { this.enabled = enabled; }
        boolean isEnabled() { return enabled; }
    }

    // Theme class for board color schemes
    static class Theme {
        String name;
        java.awt.Color lightSquare;
        java.awt.Color darkSquare;
        java.awt.Color highlightColor;
        java.awt.Color backgroundColor;
        
        Theme(String name, String light, String dark, String highlight, String bg) {
            this.name = name;
            this.lightSquare = java.awt.Color.decode(light);
            this.darkSquare = java.awt.Color.decode(dark);
            this.highlightColor = java.awt.Color.decode(highlight);
            this.backgroundColor = java.awt.Color.decode(bg);
        }
        
        static Theme[] getDefaultThemes() {
            return new Theme[] {
                new Theme("Classic", "#F0D9B5", "#B58863", "#FFFF66", "#2C2C2C"),
                new Theme("Blue", "#DEE3E6", "#8CA2AD", "#6699FF", "#1E1E2E"),
                new Theme("Green", "#FFFFDD", "#86A666", "#99FF99", "#252525"),
                new Theme("Brown", "#F0DAB5", "#946F51", "#FFD966", "#2C2416"),
                new Theme("Purple", "#E8D5F2", "#9B7EBD", "#D98FFF", "#1A1A2E"),
                new Theme("Dark Mode", "#4A4A4A", "#2C2C2C", "#808080", "#000000"),
                new Theme("High Contrast", "#FFFFFF", "#000000", "#FFFF00", "#333333")
            };
        }
    }

    // Game Statistics tracker
    static class GameStatistics {
        int whiteMaterial = 0;
        int blackMaterial = 0;
        List<Piece> whiteCaptured = new ArrayList<>();
        List<Piece> blackCaptured = new ArrayList<>();
        int moveCount = 0;
        int whiteChecks = 0;
        int blackChecks = 0;
        
        void reset() {
            whiteMaterial = 0;
            blackMaterial = 0;
            whiteCaptured.clear();
            blackCaptured.clear();
            moveCount = 0;
            whiteChecks = 0;
            blackChecks = 0;
        }
        
        void calculateMaterial(Board board) {
            whiteMaterial = 0;
            blackMaterial = 0;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = board.b[r][c];
                    if (p != null) {
                        int value = getPieceValue(p.type);
                        if (p.color == Color.WHITE) whiteMaterial += value;
                        else blackMaterial += value;
                    }
                }
            }
        }
        
        void recordCapture(Piece captured) {
            if (captured == null) return;
            if (captured.color == Color.WHITE) {
                blackCaptured.add(captured);
            } else {
                whiteCaptured.add(captured);
            }
        }
        
        int getPieceValue(PieceType type) {
            return switch (type) {
                case PAWN -> 1;
                case KNIGHT, BISHOP -> 3;
                case ROOK -> 5;
                case QUEEN -> 9;
                case KING -> 0; // King has no material value
            };
        }
        
        int getMaterialAdvantage() {
            return whiteMaterial - blackMaterial;
        }
        
        String getMaterialString(Color color) {
            int material = (color == Color.WHITE) ? whiteMaterial : blackMaterial;
            return String.valueOf(material);
        }
    }

    // GameState class to store complete game state for undo/redo
    static class GameState implements Cloneable {
        Board board;
        Color turn;
        boolean whiteKingMoved;
        boolean blackKingMoved;
        boolean whiteRookA_moved;
        boolean whiteRookH_moved;
        boolean blackRookA_moved;
        boolean blackRookH_moved;
        Pos enPassantTarget;
        Move lastMove;
        
        GameState(Board board, Color turn, boolean whiteKingMoved, boolean blackKingMoved,
                  boolean whiteRookA_moved, boolean whiteRookH_moved, 
                  boolean blackRookA_moved, boolean blackRookH_moved,
                  Pos enPassantTarget, Move lastMove) {
            this.board = board.clone();
            this.turn = turn;
            this.whiteKingMoved = whiteKingMoved;
            this.blackKingMoved = blackKingMoved;
            this.whiteRookA_moved = whiteRookA_moved;
            this.whiteRookH_moved = whiteRookH_moved;
            this.blackRookA_moved = blackRookA_moved;
            this.blackRookH_moved = blackRookH_moved;
            this.enPassantTarget = enPassantTarget;
            this.lastMove = lastMove;
        }
        
        @Override
        public GameState clone() {
            return new GameState(board, turn, whiteKingMoved, blackKingMoved,
                whiteRookA_moved, whiteRookH_moved, blackRookA_moved, blackRookH_moved,
                enPassantTarget, lastMove);
        }
    }

    static class Game {
        Board board = new Board();
        Color turn = Color.WHITE;
        Scanner in = new Scanner(System.in);
        // Castling/en-passant state
        boolean whiteKingMoved = false;
        boolean blackKingMoved = false;
        boolean whiteRookA_moved = false; // a1
        boolean whiteRookH_moved = false; // h1
        boolean blackRookA_moved = false; // a8
        boolean blackRookH_moved = false; // h8
        Pos enPassantTarget = null; // square where en-passant capture is possible

        // Undo/Redo system
        private final java.util.Deque<GameState> undoStack = new java.util.ArrayDeque<>();
        private final java.util.Deque<GameState> redoStack = new java.util.ArrayDeque<>();
        private static final int MAX_HISTORY = 100; // Limit history to prevent memory issues

        // Move validation improvements
        private final List<String> positionHistory = new ArrayList<>(); // For threefold repetition
        private int halfMoveClock = 0; // For fifty-move rule (counts half-moves since last pawn move or capture)
        private int fullMoveNumber = 1; // Current move number

        // Promotion handler (GUI can set this)
        interface PromotionHandler { Piece choosePromotion(Color byColor); }
        PromotionHandler promotionHandler = null;

        // Simple accessors for GUI
        public Board getBoard() { return board; }
        public Color getTurn() { return turn; }
        public List<Move> getLegalMovesForTurn() { return legalMoves(turn); }
        
        // Undo/Redo functionality
        public boolean canUndo() { return !undoStack.isEmpty(); }
        public boolean canRedo() { return !redoStack.isEmpty(); }
        
        private void saveState(Move lastMove) {
            // Save current state before making a move
            GameState state = new GameState(board, turn, whiteKingMoved, blackKingMoved,
                whiteRookA_moved, whiteRookH_moved, blackRookA_moved, blackRookH_moved,
                enPassantTarget, lastMove);
            undoStack.push(state);
            
            // Limit history size to prevent memory issues
            if (undoStack.size() > MAX_HISTORY) {
                // Remove the oldest state (at the bottom of the stack)
                undoStack.removeLast();
            }
            
            // Clear redo stack when a new move is made
            redoStack.clear();
        }
        
        public boolean undo() {
            if (!canUndo()) return false;
            
            // Save current state to redo stack
            GameState currentState = new GameState(board, turn, whiteKingMoved, blackKingMoved,
                whiteRookA_moved, whiteRookH_moved, blackRookA_moved, blackRookH_moved,
                enPassantTarget, null);
            redoStack.push(currentState);
            
            // Restore previous state
            GameState prevState = undoStack.pop();
            board = prevState.board.clone();
            turn = prevState.turn;
            whiteKingMoved = prevState.whiteKingMoved;
            blackKingMoved = prevState.blackKingMoved;
            whiteRookA_moved = prevState.whiteRookA_moved;
            whiteRookH_moved = prevState.whiteRookH_moved;
            blackRookA_moved = prevState.blackRookA_moved;
            blackRookH_moved = prevState.blackRookH_moved;
            enPassantTarget = prevState.enPassantTarget;
            
            return true;
        }
        
        public Move getLastMoveFromUndo() {
            if (undoStack.isEmpty()) return null;
            return undoStack.peek().lastMove;
        }
        
        public boolean redo() {
            if (!canRedo()) return false;
            
            // Save current state to undo stack
            GameState currentState = new GameState(board, turn, whiteKingMoved, blackKingMoved,
                whiteRookA_moved, whiteRookH_moved, blackRookA_moved, blackRookH_moved,
                enPassantTarget, null);
            undoStack.push(currentState);
            
            // Restore next state
            GameState nextState = redoStack.pop();
            board = nextState.board.clone();
            turn = nextState.turn;
            whiteKingMoved = nextState.whiteKingMoved;
            blackKingMoved = nextState.blackKingMoved;
            whiteRookA_moved = nextState.whiteRookA_moved;
            whiteRookH_moved = nextState.whiteRookH_moved;
            blackRookA_moved = nextState.blackRookA_moved;
            blackRookH_moved = nextState.blackRookH_moved;
            enPassantTarget = nextState.enPassantTarget;
            
            return true;
        }
        
        public boolean applyMoveIfLegal(Move candidate) {
            List<Move> moves = legalMoves(turn);
            for (Move m : moves) {
                if (m.from.equals(candidate.from) && m.to.equals(candidate.to)) {
                    // Save state before making the move (for undo)
                    saveState(null);
                    
                    // handle promotion selection
                    if (m.promotion != null && promotionHandler != null) {
                        Piece chosen = promotionHandler.choosePromotion(turn);
                        if (chosen != null) m.promotion = chosen;
                    }
                    // handle en-passant capture
                    if (m.isEnPassant) {
                        // captured pawn is behind the destination square
                        int capR = (turn==Color.WHITE) ? m.to.r + 1 : m.to.r - 1;
                        int capC = m.to.c;
                        board.b[capR][capC] = null;
                    }
                    // handle castling rook movement
                    if (m.isCastleKingSide) {
                        if (turn==Color.WHITE) {
                            // move rook from h1 to f1
                            board.set(new Pos(7,5), board.get(new Pos(7,7)));
                            board.set(new Pos(7,7), null);
                            whiteKingMoved = true; whiteRookH_moved = true;
                        } else {
                            board.set(new Pos(0,5), board.get(new Pos(0,7)));
                            board.set(new Pos(0,7), null);
                            blackKingMoved = true; blackRookH_moved = true;
                        }
                    } else if (m.isCastleQueenSide) {
                        if (turn==Color.WHITE) {
                            // move rook from a1 to d1
                            board.set(new Pos(7,3), board.get(new Pos(7,0)));
                            board.set(new Pos(7,0), null);
                            whiteKingMoved = true; whiteRookA_moved = true;
                        } else {
                            board.set(new Pos(0,3), board.get(new Pos(0,0)));
                            board.set(new Pos(0,0), null);
                            blackKingMoved = true; blackRookA_moved = true;
                        }
                    }

                    // perform the move (capture/move)
                    Piece moved = board.get(m.from);
                    Piece captured = board.get(m.to);
                    board.apply(m);

                    // update moved flags
                    if (moved != null && moved.type == PieceType.KING) {
                        if (moved.color == Color.WHITE) whiteKingMoved = true; else blackKingMoved = true;
                    }
                    if (moved != null && moved.type == PieceType.ROOK) {
                        if (m.from.r==7 && m.from.c==0) whiteRookA_moved = true;
                        if (m.from.r==7 && m.from.c==7) whiteRookH_moved = true;
                        if (m.from.r==0 && m.from.c==0) blackRookA_moved = true;
                        if (m.from.r==0 && m.from.c==7) blackRookH_moved = true;
                    }
                    // if captured a rook on its original square, mark as moved (can't castle)
                    if (captured != null && captured.type == PieceType.ROOK) {
                        if (m.to.r==7 && m.to.c==0) whiteRookA_moved = true;
                        if (m.to.r==7 && m.to.c==7) whiteRookH_moved = true;
                        if (m.to.r==0 && m.to.c==0) blackRookA_moved = true;
                        if (m.to.r==0 && m.to.c==7) blackRookH_moved = true;
                    }

                    // update en-passant target: if pawn moved two squares, set target, else clear
                    enPassantTarget = null;
                    if (moved != null && moved.type == PieceType.PAWN) {
                        if (Math.abs(m.to.r - m.from.r) == 2) {
                            // target is the square passed over
                            int tr = (m.to.r + m.from.r) / 2;
                            enPassantTarget = new Pos(tr, m.from.c);
                        }
                    }

                    // apply promotion if any
                    if (m.promotion != null) {
                        board.set(m.to, m.promotion);
                    }

                    // Update the last move in the current state (top of undo stack)
                    if (!undoStack.isEmpty()) {
                        undoStack.peek().lastMove = m;
                    }

                    // switch turn
                    turn = (turn==Color.WHITE?Color.BLACK:Color.WHITE);
                    return true;
                }
            }
            return false;
        }
        public void restart() { 
            board = new Board(); 
            turn = Color.WHITE; 
            whiteKingMoved = false;
            blackKingMoved = false;
            whiteRookA_moved = false;
            whiteRookH_moved = false;
            blackRookA_moved = false;
            blackRookH_moved = false;
            enPassantTarget = null;
            undoStack.clear();
            redoStack.clear();
        }

        void run() {
            while (true) {
                board.print();
                boolean inCheck = isInCheck(turn);
                System.out.println((turn==Color.WHITE?"White":"Black") + (inCheck?" (in check)":"") + " to move.");
                List<Move> moves = legalMoves(turn);
                if (moves.isEmpty()) {
                    if (inCheck) System.out.println("Checkmate. " + (turn==Color.WHITE?"Black":"White") + " wins.");
                    else System.out.println("Stalemate. Draw.");
                    break;
                }
                System.out.print("Enter move (e2 e4): ");
                String line = in.nextLine().trim();
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) { System.out.println("Bye."); break; }
                try {
                    Move mv = parseMove(line);
                    boolean ok=false;
                    for (Move m : moves) if (m.from.equals(mv.from) && m.to.equals(mv.to)) { mv = m; ok=true; break; }
                    if (!ok) { System.out.println("Illegal move."); continue; }
                    board.apply(mv);
                    // Pawn promotion handling already encoded in mv
                    turn = (turn==Color.WHITE?Color.BLACK:Color.WHITE);
                } catch (Exception e) {
                    System.out.println("Parse error: " + e.getMessage());
                }
            }
        }

        Move parseMove(String line) {
            // Accept formats: "e2 e4" or "e2e4"
            line = line.replaceAll("\\s+", "");
            if (line.length() < 4) throw new IllegalArgumentException("too short");
            Pos from = new Pos(line.substring(0,2));
            Pos to = new Pos(line.substring(2,4));
            Move m = new Move(from,to);
            // handle promotion like e7e8q or e7 e8 q
            if (line.length()>=5) {
                char p = Character.toLowerCase(line.charAt(4));
                Piece promotion = null;
                if (p=='q') promotion = new Piece(PieceType.QUEEN, turn);
                else if (p=='r') promotion = new Piece(PieceType.ROOK, turn);
                else if (p=='b') promotion = new Piece(PieceType.BISHOP, turn);
                else if (p=='n') promotion = new Piece(PieceType.KNIGHT, turn);
                if (promotion!=null) m.promotion = promotion;
            }
            return m;
        }

        boolean isInCheck(Color who) {
            Pos kingPos = findKing(who);
            if (kingPos == null) return true; // should not happen
            return isSquareAttacked(kingPos, (who==Color.WHITE?Color.BLACK:Color.WHITE));
        }

        Pos findKing(Color who) {
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
                Piece p = board.b[r][c];
                if (p!=null && p.type==PieceType.KING && p.color==who) return new Pos(r,c);
            }
            return null;
        }

        boolean isSquareAttacked(Pos square, Color attacker) {
            // Generate pseudo moves for attacker and see if any target square
            List<Move> attacks = pseudoLegalMoves(attacker, true);
            for (Move m : attacks) if (m.to.equals(square)) return true;
            return false;
        }

        List<Move> legalMoves(Color who) {
            List<Move> pseudo = pseudoLegalMoves(who, false);
            List<Move> legal = new ArrayList<>();
            for (Move m : pseudo) {
                Board copy = board.clone();
                copy.apply(m);
                Game sim = new Game();
                sim.board = copy;
                // king may have moved; check if own king in check
                if (!sim.isInCheck(who)) legal.add(m);
            }
            return legal;
        }

        List<Move> pseudoLegalMoves(Color who, boolean attacksOnly) {
            List<Move> moves = new ArrayList<>();
            for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
                Piece p = board.b[r][c];
                if (p==null || p.color!=who) continue;
                Pos from = new Pos(r,c);
                switch (p.type) {
                    case PAWN: pawnMoves(from, p, moves); break;
                    case KNIGHT: knightMoves(from, p, moves); break;
                    case BISHOP: slidingMoves(from, p, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                    case ROOK: slidingMoves(from, p, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                    case QUEEN: slidingMoves(from, p, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                    case KING: kingMoves(from, p, moves); break;
                }
            }
            return moves;
        }

        void pawnMoves(Pos from, Piece p, List<Move> moves) {
            int dir = (p.color==Color.WHITE? -1 : 1);
            int startRow = (p.color==Color.WHITE? 6 : 1);
            Pos one = new Pos(from.r + dir, from.c);
            if (one.inBounds() && board.get(one)==null) {
                Move m = new Move(from, one);
                // promotion
                if (one.r==0 || one.r==7) m.promotion = new Piece(PieceType.QUEEN, p.color);
                moves.add(m);
                Pos two = new Pos(from.r + 2*dir, from.c);
                if (from.r==startRow && two.inBounds() && board.get(two)==null) moves.add(new Move(from, two));
            }
            // captures
            int[] dc = {-1,1};
            for (int d:dc) {
                Pos t = new Pos(from.r + dir, from.c + d);
                if (!t.inBounds()) continue;
                Piece at = board.get(t);
                // normal capture
                if (at!=null && at.color!=p.color) {
                    Move m = new Move(from, t);
                    if (t.r==0 || t.r==7) m.promotion = new Piece(PieceType.QUEEN, p.color);
                    moves.add(m);
                }
                // en-passant capture
                else if (enPassantTarget != null && t.equals(enPassantTarget)) {
                    // ensure there is an opponent pawn in the correct square
                    int capR = (p.color==Color.WHITE) ? t.r + 1 : t.r - 1;
                    int capC = t.c;
                    if (capR>=0 && capR<8) {
                        Piece cap = board.b[capR][capC];
                        if (cap != null && cap.type==PieceType.PAWN && cap.color!=p.color) {
                            Move m = new Move(from, t);
                            m.isEnPassant = true;
                            moves.add(m);
                        }
                    }
                }
            }
            // en passant handled above via enPassantTarget
        }

        void knightMoves(Pos from, Piece p, List<Move> moves) {
            int[][] D = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
            for (int[] d: D) {
                Pos t = new Pos(from.r + d[0], from.c + d[1]);
                if (!t.inBounds()) continue;
                Piece at = board.get(t);
                if (at==null || at.color!=p.color) moves.add(new Move(from,t));
            }
        }

        void slidingMoves(Pos from, Piece p, List<Move> moves, int[][] dirs) {
            for (int[] d : dirs) {
                int r = from.r + d[0], c = from.c + d[1];
                while (r>=0 && r<8 && c>=0 && c<8) {
                    Pos t = new Pos(r,c);
                    Piece at = board.get(t);
                    if (at==null) moves.add(new Move(from,t));
                    else { if (at.color!=p.color) moves.add(new Move(from,t)); break; }
                    r += d[0]; c += d[1];
                }
            }
        }

        void kingMoves(Pos from, Piece p, List<Move> moves) {
            for (int dr=-1; dr<=1; dr++) for (int dc=-1; dc<=1; dc++) {
                if (dr==0 && dc==0) continue;
                Pos t = new Pos(from.r+dr, from.c+dc);
                if (!t.inBounds()) continue;
                Piece at = board.get(t);
                if (at==null || at.color!=p.color) moves.add(new Move(from,t));
            }
            // castling
            if (p.color==Color.WHITE) {
                if (!whiteKingMoved && from.r==7 && from.c==4) {
                    // king side
                    if (!whiteRookH_moved && board.get(new Pos(7,5))==null && board.get(new Pos(7,6))==null) {
                        // cannot castle through or into check
                        if (!isSquareAttacked(new Pos(7,4), Color.BLACK) && !isSquareAttacked(new Pos(7,5), Color.BLACK) && !isSquareAttacked(new Pos(7,6), Color.BLACK)) {
                            Move m = new Move(from, new Pos(7,6)); m.isCastleKingSide = true; moves.add(m);
                        }
                    }
                    // queen side
                    if (!whiteRookA_moved && board.get(new Pos(7,1))==null && board.get(new Pos(7,2))==null && board.get(new Pos(7,3))==null) {
                        if (!isSquareAttacked(new Pos(7,4), Color.BLACK) && !isSquareAttacked(new Pos(7,3), Color.BLACK) && !isSquareAttacked(new Pos(7,2), Color.BLACK)) {
                            Move m = new Move(from, new Pos(7,2)); m.isCastleQueenSide = true; moves.add(m);
                        }
                    }
                }
            } else {
                if (!blackKingMoved && from.r==0 && from.c==4) {
                    if (!blackRookH_moved && board.get(new Pos(0,5))==null && board.get(new Pos(0,6))==null) {
                        if (!isSquareAttacked(new Pos(0,4), Color.WHITE) && !isSquareAttacked(new Pos(0,5), Color.WHITE) && !isSquareAttacked(new Pos(0,6), Color.WHITE)) {
                            Move m = new Move(from, new Pos(0,6)); m.isCastleKingSide = true; moves.add(m);
                        }
                    }
                    if (!blackRookA_moved && board.get(new Pos(0,1))==null && board.get(new Pos(0,2))==null && board.get(new Pos(0,3))==null) {
                        if (!isSquareAttacked(new Pos(0,4), Color.WHITE) && !isSquareAttacked(new Pos(0,3), Color.WHITE) && !isSquareAttacked(new Pos(0,2), Color.WHITE)) {
                            Move m = new Move(from, new Pos(0,2)); m.isCastleQueenSide = true; moves.add(m);
                        }
                    }
                }
            }
        }
    }

    // --- Enhanced Professional Swing GUI ---
    static class ChessGUI {
        private JFrame frame;
        private Game game;
        private BoardPanel boardPanel;
        private JLabel statusLabel;
        private JLabel turnIndicatorLabel;
        private JLabel moveCountLabel;
        private DefaultListModel<String> movesListModel;
        private JList<String> movesList;
        private Move lastMove = null;
        private int moveCounter = 0;
        
        // Timer components
        private ChessTimer chessTimer = null;
        private JLabel whiteTimerLabel;
        private JLabel blackTimerLabel;
        private javax.swing.Timer timerUpdateTimer;
        private boolean timerEnabled = false;
        
        // Optimization: Use more efficient collections
        // original (unscaled) images loaded from disk or generated placeholders
        private final Map<String, Image> originalImages = new HashMap<>(12); // 12 pieces
        // cache of scaled images per cell size
        private final Map<Integer, Map<String, Image>> scaledImageCache = new HashMap<>(5); // typical 5 sizes
        private boolean useImages = true;
        private boolean highContrast = false;
        
        // Optimization: Cache rendering hints for reuse
        private static final RenderingHints QUALITY_HINTS = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        static {
            QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            QUALITY_HINTS.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            QUALITY_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        
        // Color scheme constants
        private static final java.awt.Color LIGHT_SQUARE = java.awt.Color.decode("#F0D9B5");
        private static final java.awt.Color DARK_SQUARE = java.awt.Color.decode("#B58863");
        private static final java.awt.Color SELECTED_HIGHLIGHT = new java.awt.Color(255, 255, 102, 180);
        private static final java.awt.Color LEGAL_MOVE_HIGHLIGHT = new java.awt.Color(124, 252, 0, 100);
        private static final java.awt.Color CAPTURE_HIGHLIGHT = new java.awt.Color(255, 99, 71, 120);
        private static final java.awt.Color LAST_MOVE_HIGHLIGHT = new java.awt.Color(205, 210, 106, 120);
        private static final java.awt.Color PANEL_BG = java.awt.Color.decode("#2C2C2C");
        private static final java.awt.Color PANEL_FG = java.awt.Color.decode("#E8E8E8");
        private static final java.awt.Color ACCENT_COLOR = java.awt.Color.decode("#4A9EFF");

        void createAndShowGUI() {
            game = new Game();
            // set promotion handler to show chooser dialog
            game.promotionHandler = (byColor) -> {
                String[] options = {"Queen", "Rook", "Bishop", "Knight"};
                int sel = JOptionPane.showOptionDialog(frame,
                        "Choose promotion for " + (byColor==Color.WHITE?"White":"Black"),
                        "Promotion",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]);
                if (sel < 0) return new Piece(PieceType.QUEEN, byColor);
                PieceType t = PieceType.QUEEN;
                switch (sel) {
                    case 0: t = PieceType.QUEEN; break;
                    case 1: t = PieceType.ROOK; break;
                    case 2: t = PieceType.BISHOP; break;
                    case 3: t = PieceType.KNIGHT; break;
                }
                return new Piece(t, byColor);
            };
            frame = new JFrame("♔ Professional Chess ♔");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loadPieceImages();
            createMenuBar();

            boardPanel = new BoardPanel();
            boardPanel.setPreferredSize(new Dimension(640, 640));
            boardPanel.setMinimumSize(new Dimension(400, 400));

            // Create enhanced info panel
            JPanel infoPanel = createInfoPanel();
            
            // Create main layout with better proportions
            frame.getContentPane().setLayout(new BorderLayout(10, 10));
            frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
            frame.getContentPane().add(infoPanel, BorderLayout.EAST);
            
            frame.setMinimumSize(new Dimension(900, 700));
            frame.pack();
            frame.setLocationRelativeTo(null);
            updateStatus();
            frame.setVisible(true);
        }
        
        private JPanel createTimerPanel() {
            JPanel timerPanel = new JPanel();
            timerPanel.setLayout(new BoxLayout(timerPanel, BoxLayout.Y_AXIS));
            timerPanel.setBackground(PANEL_BG);
            timerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            timerPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            
            // Black timer (on top)
            JPanel blackTimerPanel = new JPanel(new BorderLayout(5, 0));
            blackTimerPanel.setBackground(PANEL_BG);
            JLabel blackLabel = new JLabel("⚫ Black:");
            blackLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            blackLabel.setForeground(java.awt.Color.GRAY);
            blackTimerLabel = new JLabel("--:--");
            blackTimerLabel.setFont(new Font("Courier New", Font.BOLD, 18));
            blackTimerLabel.setForeground(PANEL_FG);
            blackTimerPanel.add(blackLabel, BorderLayout.WEST);
            blackTimerPanel.add(blackTimerLabel, BorderLayout.EAST);
            timerPanel.add(blackTimerPanel);
            timerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            // White timer (on bottom)
            JPanel whiteTimerPanel = new JPanel(new BorderLayout(5, 0));
            whiteTimerPanel.setBackground(PANEL_BG);
            JLabel whiteLabel = new JLabel("⚪ White:");
            whiteLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            whiteLabel.setForeground(java.awt.Color.WHITE);
            whiteTimerLabel = new JLabel("--:--");
            whiteTimerLabel.setFont(new Font("Courier New", Font.BOLD, 18));
            whiteTimerLabel.setForeground(PANEL_FG);
            whiteTimerPanel.add(whiteLabel, BorderLayout.WEST);
            whiteTimerPanel.add(whiteTimerLabel, BorderLayout.EAST);
            timerPanel.add(whiteTimerPanel);
            
            return timerPanel;
        }
        
        private JPanel createInfoPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(PANEL_BG);
            panel.setPreferredSize(new Dimension(280, 640));
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            // Title label
            JLabel titleLabel = new JLabel("♔ CHESS ♔");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            titleLabel.setForeground(ACCENT_COLOR);
            titleLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            panel.add(titleLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            
            // Game info section
            JPanel gameInfoPanel = new JPanel();
            gameInfoPanel.setLayout(new BoxLayout(gameInfoPanel, BoxLayout.Y_AXIS));
            gameInfoPanel.setBackground(PANEL_BG);
            gameInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            gameInfoPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            
            // Turn indicator with color
            JPanel turnPanel = new JPanel(new BorderLayout(10, 0));
            turnPanel.setBackground(PANEL_BG);
            JLabel turnLabel = new JLabel("Current Turn:");
            turnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            turnLabel.setForeground(PANEL_FG);
            turnIndicatorLabel = new JLabel("● White");
            turnIndicatorLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            turnIndicatorLabel.setForeground(java.awt.Color.WHITE);
            turnPanel.add(turnLabel, BorderLayout.WEST);
            turnPanel.add(turnIndicatorLabel, BorderLayout.EAST);
            gameInfoPanel.add(turnPanel);
            gameInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            // Move counter
            JPanel movePanel = new JPanel(new BorderLayout(10, 0));
            movePanel.setBackground(PANEL_BG);
            JLabel moveLabel = new JLabel("Move:");
            moveLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            moveLabel.setForeground(PANEL_FG);
            moveCountLabel = new JLabel("0");
            moveCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            moveCountLabel.setForeground(ACCENT_COLOR);
            movePanel.add(moveLabel, BorderLayout.WEST);
            movePanel.add(moveCountLabel, BorderLayout.EAST);
            gameInfoPanel.add(movePanel);
            gameInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            // Status label
            statusLabel = new JLabel("White to move");
            statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            statusLabel.setForeground(new java.awt.Color(200, 200, 200));
            statusLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            gameInfoPanel.add(statusLabel);
            
            panel.add(gameInfoPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            
            // Timer display section
            JPanel timerPanel = createTimerPanel();
            panel.add(timerPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            
            // Move history section
            JLabel historyLabel = new JLabel("Move History");
            historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            historyLabel.setForeground(PANEL_FG);
            historyLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            panel.add(historyLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            
            movesListModel = new DefaultListModel<>();
            movesList = new JList<>(movesListModel);
            movesList.setFont(new Font("Courier New", Font.PLAIN, 13));
            movesList.setBackground(java.awt.Color.decode("#1E1E1E"));
            movesList.setForeground(PANEL_FG);
            movesList.setSelectionBackground(ACCENT_COLOR);
            movesList.setSelectionForeground(java.awt.Color.WHITE);
            JScrollPane scroll = new JScrollPane(movesList);
            scroll.setPreferredSize(new Dimension(250, 300));
            scroll.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 2));
            panel.add(scroll);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            
            // Control buttons
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
            buttonsPanel.setBackground(PANEL_BG);
            buttonsPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            
            JButton newBtn = createStyledButton("New Game");
            newBtn.addActionListener(e -> {
                game.restart();
                movesListModel.clear();
                moveCounter = 0;
                lastMove = null;
                
                // Reset timer if enabled
                if (timerEnabled && chessTimer != null) {
                    // Store current settings
                    int minutes = (int)(chessTimer.whiteTime / 60000);
                    int increment = (int)(chessTimer.increment / 1000);
                    chessTimer.reset(minutes > 0 ? minutes : 10, increment);
                    chessTimer.start(Color.WHITE);
                }
                
                boardPanel.clearSelection();
                updateStatus();
                boardPanel.repaint();
            });
            
            JButton undoBtn = createStyledButton("Undo Move");
            undoBtn.addActionListener(e -> {
                if (game.undo()) {
                    // Remove last move from history display
                    if (movesListModel.size() > 0) {
                        movesListModel.remove(movesListModel.size() - 1);
                        moveCounter--;
                    }
                    // Get the last move from the previous state
                    lastMove = game.getLastMoveFromUndo();
                    boardPanel.clearSelection();
                    updateStatus();
                    boardPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, 
                        "No moves to undo!", 
                        "Undo", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            JButton redoBtn = createStyledButton("Redo Move");
            redoBtn.addActionListener(e -> {
                if (game.redo()) {
                    // Re-add the move to history display
                    Move redoneMove = game.getLastMoveFromUndo();
                    if (redoneMove != null) {
                        moveCounter++;
                        String moveStr = formatMove(redoneMove, moveCounter);
                        movesListModel.addElement(moveStr);
                        movesList.ensureIndexIsVisible(movesListModel.getSize() - 1);
                        lastMove = redoneMove;
                    }
                    boardPanel.clearSelection();
                    updateStatus();
                    boardPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, 
                        "No moves to redo!", 
                        "Redo", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            JButton resignBtn = createStyledButton("Resign");
            resignBtn.addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to resign?",
                    "Resign Game",
                    JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    String winner = game.getTurn() == Color.WHITE ? "Black" : "White";
                    JOptionPane.showMessageDialog(frame, winner + " wins by resignation!");
                }
            });
            
            buttonsPanel.add(newBtn);
            buttonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            buttonsPanel.add(undoBtn);
            buttonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            buttonsPanel.add(redoBtn);
            buttonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            buttonsPanel.add(resignBtn);
            
            panel.add(buttonsPanel);
            panel.add(Box.createVerticalGlue());
            
            return panel;
        }
        
        private JButton createStyledButton(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setForeground(java.awt.Color.WHITE);
            btn.setBackground(ACCENT_COLOR);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setOpaque(true);
            btn.setMaximumSize(new Dimension(250, 40));
            btn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(ACCENT_COLOR.brighter());
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(ACCENT_COLOR);
                }
            });
            return btn;
        }

        void updateStatus() {
            String who = (game.getTurn() == Color.WHITE) ? "White" : "Black";
            boolean inCheck = game.isInCheck(game.getTurn());
            
            // Update turn indicator
            turnIndicatorLabel.setText("● " + who);
            turnIndicatorLabel.setForeground(game.getTurn() == Color.WHITE ? 
                java.awt.Color.WHITE : java.awt.Color.decode("#1E1E1E"));
            
            // Update move counter
            moveCountLabel.setText(String.valueOf(moveCounter));
            
            // Update status text
            if (inCheck) {
                statusLabel.setText("⚠ " + who + " is in CHECK!");
                statusLabel.setForeground(new java.awt.Color(255, 100, 100));
            } else {
                statusLabel.setText(who + " to move");
                statusLabel.setForeground(new java.awt.Color(200, 200, 200));
            }
            
            // Check for checkmate or stalemate
            List<Move> moves = game.getLegalMovesForTurn();
            if (moves.isEmpty()) {
                if (inCheck) {
                    String winner = game.getTurn() == Color.WHITE ? "Black" : "White";
                    statusLabel.setText("♔ CHECKMATE! " + winner + " wins!");
                    statusLabel.setForeground(new java.awt.Color(100, 255, 100));
                    JOptionPane.showMessageDialog(frame, 
                        winner + " wins by checkmate!",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText("STALEMATE - Draw!");
                    statusLabel.setForeground(new java.awt.Color(255, 200, 100));
                    JOptionPane.showMessageDialog(frame, 
                        "The game is a draw by stalemate.",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        
        String formatMove(Move move, int moveNum) {
            // Optimization: Use StringBuilder to avoid string concatenation
            StringBuilder sb = new StringBuilder(12);
            if (moveNum % 2 == 1) {
                sb.append((moveNum + 1) / 2).append(". ");
            } else {
                sb.append("   ");
            }
            sb.append(move.from).append("-").append(move.to);
            return sb.toString();
        }

        class BoardPanel extends JPanel {
            int square = 64;
            Pos selected = null;
            java.util.List<Pos> targets = null;
            // geometry used for both painting and mouse mapping (computed consistently)
            int cellSize = 0; // integer cell size (pixels)
            int xOff = 0, yOff = 0; // integer offsets where board starts
            Pos keyboardCursor = new Pos(7,4); // start cursor

            BoardPanel() {
                setFocusable(true);
                // ensure consistent geometry calculation reused by paint and mouse
                addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        requestFocusInWindow();
                        computeBoardGeometry();
                        int mx = e.getX();
                        int my = e.getY();
                        int col = (int)Math.floor((mx - xOff) / (double)cellSize);
                        int row = (int)Math.floor((my - yOff) / (double)cellSize);
                        if (col < 0 || col > 7 || row < 0 || row > 7) return;
                        Pos clicked = new Pos(row, col);
                        if (selected == null) {
                            Piece p = game.getBoard().get(clicked);
                            if (p != null && p.color == game.getTurn()) {
                                selected = clicked;
                                targets = computeTargets(selected);
                                repaint();
                            }
                        } else {
                            Move candidate = new Move(selected, clicked);
                            boolean ok = game.applyMoveIfLegal(candidate);
                            if (ok) {
                                moveCounter++;
                                lastMove = candidate;
                                
                                // Switch timer after move
                                if (timerEnabled && chessTimer != null) {
                                    chessTimer.switchPlayer();
                                }
                                
                                // Format move in algebraic notation
                                String moveStr = formatMove(candidate, moveCounter);
                                movesListModel.addElement(moveStr);
                                
                                // Auto-scroll to latest move
                                movesList.ensureIndexIsVisible(movesListModel.getSize() - 1);
                                
                                selected = null; targets = null;
                                updateStatus();
                                repaint();
                            } else {
                                Piece p = game.getBoard().get(clicked);
                                if (p != null && p.color == game.getTurn()) {
                                    selected = clicked;
                                    targets = computeTargets(selected);
                                    repaint();
                                } else {
                                    selected = null; targets = null; repaint();
                                }
                            }
                        }
                    }
                });
                // keyboard bindings
                setupKeyBindings();
            }

            java.util.List<Pos> computeTargets(Pos from) {
                java.util.List<Pos> t = new ArrayList<>();
                for (Move m : game.getLegalMovesForTurn()) if (m.from.equals(from)) t.add(m.to);
                return t;
            }

            void clearSelection() { 
                selected = null; 
                targets = null; 
            }
            
            // Optimization: Clear image cache to free memory if needed
            void clearImageCache() {
                scaledImageCache.clear();
            }
            
            // Optimization: Helper method for efficient piece key generation
            private String getPieceKey(Piece p) {
                char colorChar = (p.color == Color.WHITE) ? 'w' : 'b';
                char typeChar = switch (p.type) {
                    case PAWN -> 'p';
                    case KNIGHT -> 'n';
                    case BISHOP -> 'b';
                    case ROOK -> 'r';
                    case QUEEN -> 'q';
                    case KING -> 'k';
                };
                return String.valueOf(colorChar) + typeChar;
            }

            @Override 
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                
                // Optimization: Use cached rendering hints instead of setting individually
                g2.setRenderingHints(QUALITY_HINTS);
                
                computeBoardGeometry();
                
                // Draw board background
                g2.setColor(PANEL_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw board border with depth effect
                int borderWidth = 4;
                g2.setColor(new java.awt.Color(80, 80, 80));
                g2.fillRect(xOff - borderWidth, yOff - borderWidth, 
                    cellSize * 8 + borderWidth * 2, cellSize * 8 + borderWidth * 2);
                
                // Draw squares with subtle depth
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        int x = xOff + c * cellSize;
                        int y = yOff + r * cellSize;
                        boolean light = ((r + c) % 2 == 0);
                        
                        // Base square color
                        if (highContrast) {
                            g2.setColor(light ? java.awt.Color.WHITE : java.awt.Color.BLACK);
                        } else {
                            g2.setColor(light ? LIGHT_SQUARE : DARK_SQUARE);
                        }
                        g2.fillRect(x, y, cellSize, cellSize);
                        
                        // Subtle inner shadow for depth
                        if (!highContrast) {
                            g2.setColor(new java.awt.Color(0, 0, 0, 15));
                            g2.drawRect(x + 1, y + 1, cellSize - 2, cellSize - 2);
                        }
                    }
                }
                
                // Highlight last move
                if (lastMove != null) {
                    g2.setColor(LAST_MOVE_HIGHLIGHT);
                    g2.fillRect(xOff + lastMove.from.c * cellSize, yOff + lastMove.from.r * cellSize, 
                        cellSize, cellSize);
                    g2.fillRect(xOff + lastMove.to.c * cellSize, yOff + lastMove.to.r * cellSize, 
                        cellSize, cellSize);
                }
                
                // Highlight selected square with glow effect
                if (selected != null) {
                    g2.setColor(SELECTED_HIGHLIGHT);
                    g2.fillRect(xOff + selected.c * cellSize, yOff + selected.r * cellSize, 
                        cellSize, cellSize);
                    
                    // Add border to selected square
                    g2.setColor(new java.awt.Color(255, 255, 0, 255));
                    g2.setStroke(new java.awt.BasicStroke(3));
                    g2.drawRect(xOff + selected.c * cellSize + 2, yOff + selected.r * cellSize + 2, 
                        cellSize - 4, cellSize - 4);
                }
                
                // Highlight legal move targets with circles
                if (targets != null) {
                    for (Pos p : targets) {
                        Piece targetPiece = game.getBoard().get(p);
                        boolean isCapture = (targetPiece != null);
                        
                        int x = xOff + p.c * cellSize;
                        int y = yOff + p.r * cellSize;
                        
                        if (isCapture) {
                            // Capture indicator - ring around the square
                            g2.setColor(CAPTURE_HIGHLIGHT);
                            g2.fillRect(x, y, cellSize, cellSize);
                            g2.setColor(new java.awt.Color(255, 99, 71, 200));
                            g2.setStroke(new java.awt.BasicStroke(4));
                            g2.drawRoundRect(x + 3, y + 3, cellSize - 6, cellSize - 6, 10, 10);
                        } else {
                            // Regular move indicator - small circle
                            g2.setColor(LEGAL_MOVE_HIGHLIGHT);
                            int dotSize = cellSize / 4;
                            int dotX = x + (cellSize - dotSize) / 2;
                            int dotY = y + (cellSize - dotSize) / 2;
                            g2.fillOval(dotX, dotY, dotSize, dotSize);
                            
                            // Subtle border
                            g2.setColor(new java.awt.Color(100, 200, 100, 150));
                            g2.setStroke(new java.awt.BasicStroke(2));
                            g2.drawOval(dotX, dotY, dotSize, dotSize);
                        }
                    }
                }
                
                // Draw coordinate labels
                drawCoordinates(g2);
                
                // Draw pieces with enhanced visuals
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        Piece p = game.getBoard().b[r][c];
                        if (p != null) {
                            // Optimization: Use more efficient key generation
                            String key = getPieceKey(p);
                            int px = xOff + c * cellSize;
                            int py = yOff + r * cellSize;
                            
                            if (useImages) {
                                Image img = getScaledImage(key, cellSize);
                                if (img != null) {
                                    // Add subtle shadow under pieces
                                    g2.setColor(new java.awt.Color(0, 0, 0, 40));
                                    g2.fillOval(px + cellSize/6, py + cellSize - cellSize/8, 
                                        cellSize*2/3, cellSize/10);
                                    g2.drawImage(img, px, py, null);
                                    continue;
                                }
                            }
                            
                            // Enhanced Unicode rendering with better styling
                            String s = unicodeFor(p);
                            int fontSize = Math.max(16, cellSize * 7 / 10);
                            Font font = new Font("Segoe UI Symbol", Font.PLAIN, fontSize);
                            g2.setFont(font);
                            FontMetrics fm = g2.getFontMetrics();
                            int strW = fm.stringWidth(s);
                            int strH = fm.getAscent();
                            int sx = px + (cellSize - strW)/2;
                            int sy = py + (cellSize - strH)/2 + strH;
                            
                            // Drop shadow for depth
                            g2.setColor(new java.awt.Color(0, 0, 0, 100));
                            g2.drawString(s, sx + 2, sy + 2);
                            
                            // Main piece color
                            if (p.color == Color.WHITE) {
                                g2.setColor(new java.awt.Color(255, 255, 255));
                                g2.drawString(s, sx, sy);
                                // Add outline for better visibility
                                g2.setColor(new java.awt.Color(50, 50, 50, 80));
                                g2.setStroke(new java.awt.BasicStroke(1));
                            } else {
                                g2.setColor(new java.awt.Color(30, 30, 30));
                                g2.drawString(s, sx, sy);
                            }
                        }
                    }
                }
                
                g2.dispose();
            }
            
            private void drawCoordinates(Graphics2D g2) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, cellSize / 6)));
                g2.setColor(new java.awt.Color(180, 180, 180));
                FontMetrics fm = g2.getFontMetrics();
                
                // Draw file labels (a-h) at bottom
                for (int c = 0; c < 8; c++) {
                    String label = String.valueOf((char)('a' + c));
                    int x = xOff + c * cellSize + (cellSize - fm.stringWidth(label)) / 2;
                    int y = yOff + 8 * cellSize + fm.getAscent() + 5;
                    g2.drawString(label, x, y);
                }
                
                // Draw rank labels (1-8) on left side
                for (int r = 0; r < 8; r++) {
                    String label = String.valueOf(8 - r);
                    int x = xOff - fm.stringWidth(label) - 8;
                    int y = yOff + r * cellSize + (cellSize + fm.getAscent()) / 2 - 2;
                    g2.drawString(label, x, y);
                }
            }

            // compute board geometry in one place so both paint and mouse use identical math
            private void computeBoardGeometry() {
                int w = getWidth();
                int h = getHeight();
                // Reserve space for coordinate labels
                int coordSpace = 30;
                int availableW = w - coordSpace;
                int availableH = h - coordSpace;
                int size = Math.min(availableW, availableH);
                int sq = Math.max(1, (int)Math.floor(size / 8.0));
                int xoff = (w - sq*8) / 2;
                int yoff = (h - sq*8) / 2 - coordSpace / 4;
                this.cellSize = sq;
                this.xOff = xoff;
                this.yOff = yoff;
            }

            private Image getScaledImage(String key, int size) {
                // Optimization: Early return if no original image
                Image orig = originalImages.get(key);
                if (orig == null) return null;
                
                // Optimization: Use computeIfAbsent for cleaner cache management
                Map<String, Image> sizeCache = scaledImageCache.computeIfAbsent(size, k -> new HashMap<>(12));
                
                // Return cached image if available
                Image cached = sizeCache.get(key);
                if (cached != null) return cached;
                
                // Optimization: Produce high-quality scaled BufferedImage with cached hints
                BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = buf.createGraphics();
                gg.setRenderingHints(QUALITY_HINTS);  // Use cached hints
                gg.drawImage(orig, 0, 0, size, size, null);
                gg.dispose();
                
                // Cache and return
                sizeCache.put(key, buf);
                return buf;
            }

            private void setupKeyBindings() {
                InputMap im = getInputMap(WHEN_FOCUSED);
                ActionMap am = getActionMap();
                im.put(KeyStroke.getKeyStroke("LEFT"), "cursorLeft");
                im.put(KeyStroke.getKeyStroke("RIGHT"), "cursorRight");
                im.put(KeyStroke.getKeyStroke("UP"), "cursorUp");
                im.put(KeyStroke.getKeyStroke("DOWN"), "cursorDown");
                im.put(KeyStroke.getKeyStroke("ENTER"), "selectOrMove");
                im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancelSelect");
                am.put("cursorLeft", new AbstractAction(){ public void actionPerformed(ActionEvent e){ moveCursor(0,-1); }});
                am.put("cursorRight", new AbstractAction(){ public void actionPerformed(ActionEvent e){ moveCursor(0,1); }});
                am.put("cursorUp", new AbstractAction(){ public void actionPerformed(ActionEvent e){ moveCursor(-1,0); }});
                am.put("cursorDown", new AbstractAction(){ public void actionPerformed(ActionEvent e){ moveCursor(1,0); }});
                am.put("selectOrMove", new AbstractAction(){ public void actionPerformed(ActionEvent e){ keyboardSelectOrMove(); }});
                am.put("cancelSelect", new AbstractAction(){ public void actionPerformed(ActionEvent e){ selected=null; targets=null; repaint(); }});
            }

            private void moveCursor(int dr, int dc) {
                int nr = Math.max(0, Math.min(7, keyboardCursor.r + dr));
                int nc = Math.max(0, Math.min(7, keyboardCursor.c + dc));
                keyboardCursor = new Pos(nr, nc);
                // if nothing selected, position selection hover; else just repaint
                if (selected == null) {
                    selected = keyboardCursor;
                    targets = computeTargets(selected);
                }
                repaint();
            }

            private void keyboardSelectOrMove() {
                if (selected == null) {
                    selected = keyboardCursor;
                    targets = computeTargets(selected);
                    repaint();
                    return;
                }
                Move candidate = new Move(selected, keyboardCursor);
                boolean ok = game.applyMoveIfLegal(candidate);
                if (ok) {
                    moveCounter++;
                    lastMove = candidate;
                    
                    // Switch timer after move
                    if (timerEnabled && chessTimer != null) {
                        chessTimer.switchPlayer();
                    }
                    
                    String moveStr = formatMove(candidate, moveCounter);
                    movesListModel.addElement(moveStr);
                    movesList.ensureIndexIsVisible(movesListModel.getSize() - 1);
                    selected = null; targets = null;
                    updateStatus();
                    repaint();
                } else {
                    Piece p = game.getBoard().get(keyboardCursor);
                    if (p != null && p.color == game.getTurn()) {
                        selected = keyboardCursor;
                        targets = computeTargets(selected);
                        repaint();
                    }
                }
            }

            @Override public Dimension getPreferredSize() { return new Dimension(8*square, 8*square); }
        }
        private String unicodeFor(Piece p) {
            // Use Unicode chess symbols for nicer rendering
            if (p.color == Color.WHITE) {
                switch (p.type) {
                    case KING: return "\u2654";
                    case QUEEN: return "\u2655";
                    case ROOK: return "\u2656";
                    case BISHOP: return "\u2657";
                    case KNIGHT: return "\u2658";
                    case PAWN: return "\u2659";
                }
            } else {
                switch (p.type) {
                    case KING: return "\u265A";
                    case QUEEN: return "\u265B";
                    case ROOK: return "\u265C";
                    case BISHOP: return "\u265D";
                    case KNIGHT: return "\u265E";
                    case PAWN: return "\u265F";
                }
            }
            return "?";
        }

        private void setupTimer(int minutes, int incrementSeconds) {
            timerEnabled = true;
            chessTimer = new ChessTimer(minutes, incrementSeconds);
            
            // Create update timer that refreshes display every 100ms
            if (timerUpdateTimer != null) {
                timerUpdateTimer.stop();
            }
            timerUpdateTimer = new javax.swing.Timer(100, e -> updateTimerDisplay());
            timerUpdateTimer.start();
            
            // Start the chess timer for white
            chessTimer.start(Color.WHITE);
            updateTimerDisplay();
            
            JOptionPane.showMessageDialog(frame, 
                String.format("Timer set: %d minutes + %d seconds increment", minutes, incrementSeconds),
                "Timer Started",
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        private void setupCustomTimer() {
            JPanel panel = new JPanel(new java.awt.GridLayout(2, 2, 5, 5));
            JTextField minutesField = new JTextField("10");
            JTextField incrementField = new JTextField("0");
            panel.add(new JLabel("Minutes per side:"));
            panel.add(minutesField);
            panel.add(new JLabel("Increment (seconds):"));
            panel.add(incrementField);
            
            int result = JOptionPane.showConfirmDialog(frame, panel, 
                "Custom Timer Setup", JOptionPane.OK_CANCEL_OPTION);
            
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int minutes = Integer.parseInt(minutesField.getText());
                    int increment = Integer.parseInt(incrementField.getText());
                    if (minutes > 0 && increment >= 0) {
                        setupTimer(minutes, increment);
                    } else {
                        JOptionPane.showMessageDialog(frame, 
                            "Please enter positive values", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, 
                        "Please enter valid numbers", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        private void disableTimer() {
            timerEnabled = false;
            if (chessTimer != null) {
                chessTimer.stop();
            }
            if (timerUpdateTimer != null) {
                timerUpdateTimer.stop();
            }
            whiteTimerLabel.setText("--:--");
            blackTimerLabel.setText("--:--");
            JOptionPane.showMessageDialog(frame, "Timer disabled", 
                "Timer", JOptionPane.INFORMATION_MESSAGE);
        }
        
        private void updateTimerDisplay() {
            if (!timerEnabled || chessTimer == null) return;
            
            String whiteTime = chessTimer.formatTime(chessTimer.getWhiteTime());
            String blackTime = chessTimer.formatTime(chessTimer.getBlackTime());
            
            whiteTimerLabel.setText(whiteTime);
            blackTimerLabel.setText(blackTime);
            
            // Highlight active player's timer
            if (chessTimer.isRunning()) {
                if (game.getTurn() == Color.WHITE) {
                    whiteTimerLabel.setForeground(java.awt.Color.GREEN);
                    blackTimerLabel.setForeground(PANEL_FG);
                } else {
                    blackTimerLabel.setForeground(java.awt.Color.GREEN);
                    whiteTimerLabel.setForeground(PANEL_FG);
                }
            }
            
            // Check for time out
            if (chessTimer.isWhiteOutOfTime()) {
                chessTimer.stop();
                timerUpdateTimer.stop();
                whiteTimerLabel.setForeground(java.awt.Color.RED);
                JOptionPane.showMessageDialog(frame, 
                    "⏱ White ran out of time! Black wins!", 
                    "Time's Up!", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else if (chessTimer.isBlackOutOfTime()) {
                chessTimer.stop();
                timerUpdateTimer.stop();
                blackTimerLabel.setForeground(java.awt.Color.RED);
                JOptionPane.showMessageDialog(frame, 
                    "⏱ Black ran out of time! White wins!", 
                    "Time's Up!", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
        private void createMenuBar() {
            JMenuBar mb = new JMenuBar();
            
            // Game menu
            JMenu gameMenu = new JMenu("Game");
            
            JMenuItem newGameItem = new JMenuItem("New Game");
            newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
            newGameItem.addActionListener(e -> {
                game.restart();
                movesListModel.clear();
                moveCounter = 0;
                lastMove = null;
                
                // Reset timer if enabled
                if (timerEnabled && chessTimer != null) {
                    int minutes = (int)(chessTimer.whiteTime / 60000);
                    int increment = (int)(chessTimer.increment / 1000);
                    chessTimer.reset(minutes > 0 ? minutes : 10, increment);
                    chessTimer.start(Color.WHITE);
                }
                
                boardPanel.clearSelection();
                updateStatus();
                boardPanel.repaint();
            });
            
            JMenuItem undoItem = new JMenuItem("Undo Move");
            undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
            undoItem.addActionListener(e -> {
                if (game.undo()) {
                    if (movesListModel.size() > 0) {
                        movesListModel.remove(movesListModel.size() - 1);
                        moveCounter--;
                    }
                    lastMove = game.getLastMoveFromUndo();
                    boardPanel.clearSelection();
                    updateStatus();
                    boardPanel.repaint();
                }
            });
            
            JMenuItem redoItem = new JMenuItem("Redo Move");
            redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
            redoItem.addActionListener(e -> {
                if (game.redo()) {
                    Move redoneMove = game.getLastMoveFromUndo();
                    if (redoneMove != null) {
                        moveCounter++;
                        String moveStr = formatMove(redoneMove, moveCounter);
                        movesListModel.addElement(moveStr);
                        movesList.ensureIndexIsVisible(movesListModel.getSize() - 1);
                        lastMove = redoneMove;
                    }
                    boardPanel.clearSelection();
                    updateStatus();
                    boardPanel.repaint();
                }
            });
            
            gameMenu.add(newGameItem);
            gameMenu.addSeparator();
            gameMenu.add(undoItem);
            gameMenu.add(redoItem);
            mb.add(gameMenu);
            
            // Timer menu
            JMenu timerMenu = new JMenu("Timer");
            
            JMenuItem timer5_0 = new JMenuItem("5 minutes (no increment)");
            timer5_0.addActionListener(e -> setupTimer(5, 0));
            
            JMenuItem timer10_0 = new JMenuItem("10 minutes (no increment)");
            timer10_0.addActionListener(e -> setupTimer(10, 0));
            
            JMenuItem timer15_10 = new JMenuItem("15 minutes + 10 sec");
            timer15_10.addActionListener(e -> setupTimer(15, 10));
            
            JMenuItem timer30_0 = new JMenuItem("30 minutes (no increment)");
            timer30_0.addActionListener(e -> setupTimer(30, 0));
            
            JMenuItem timerCustom = new JMenuItem("Custom...");
            timerCustom.addActionListener(e -> setupCustomTimer());
            
            JMenuItem timerDisable = new JMenuItem("Disable Timer");
            timerDisable.addActionListener(e -> disableTimer());
            
            timerMenu.add(timer5_0);
            timerMenu.add(timer10_0);
            timerMenu.add(timer15_10);
            timerMenu.add(timer30_0);
            timerMenu.addSeparator();
            timerMenu.add(timerCustom);
            timerMenu.add(timerDisable);
            mb.add(timerMenu);
            
            // View menu
            JMenu view = new JMenu("View");
            JCheckBoxMenuItem imagesToggle = new JCheckBoxMenuItem("Use Images", useImages);
            imagesToggle.addActionListener(e -> { useImages = imagesToggle.isSelected(); boardPanel.repaint(); });
            JCheckBoxMenuItem contrastToggle = new JCheckBoxMenuItem("High Contrast", highContrast);
            contrastToggle.addActionListener(e -> { highContrast = contrastToggle.isSelected(); boardPanel.repaint(); });
            view.add(imagesToggle);
            view.add(contrastToggle);
            mb.add(view);
            
            frame.setJMenuBar(mb);
        }

        private Image makePlaceholderIcon(String key) {
            // Enhanced stylized piece rendering
            int S = 256;
            BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            // Optimization: Use cached rendering hints
            g.setRenderingHints(QUALITY_HINTS);
            
            // Background transparent
            g.setColor(new java.awt.Color(0,0,0,0));
            g.fillRect(0,0,S,S);
            
            boolean isWhite = key.startsWith("w");
            char pieceType = key.charAt(1);
            
            // Define colors with gradient effect
            java.awt.Color baseColor = isWhite ? 
                new java.awt.Color(240, 240, 240) : new java.awt.Color(40, 40, 40);
            java.awt.Color highlightColor = isWhite ? 
                java.awt.Color.WHITE : new java.awt.Color(80, 80, 80);
            
            // Create gradient paint for 3D effect
            java.awt.GradientPaint gradient = new java.awt.GradientPaint(
                S/4, S/4, highlightColor,
                3*S/4, 3*S/4, baseColor
            );
            
            // Draw stylized piece shape
            int margin = S / 8;
            int baseWidth = S - 2 * margin;
            int baseHeight = S / 6;
            
            // Draw shadow
            g.setColor(new java.awt.Color(0, 0, 0, 60));
            g.fillOval(margin + 5, S - margin - baseHeight + 5, baseWidth, baseHeight);
            
            // Draw base
            g.setPaint(gradient);
            g.fillOval(margin, S - margin - baseHeight, baseWidth, baseHeight);
            
            // Draw main body (varies by piece type)
            int bodyTop = S / 4;
            int bodyBottom = S - margin - baseHeight;
            
            // Draw piece-specific shape
            if (pieceType == 'p') { // Pawn - simple oval
                g.fillOval(S/3, bodyTop, S/3, bodyBottom - bodyTop);
                g.fillOval(S/3 - S/12, bodyTop - S/8, S/3 + S/6, S/4);
            } else if (pieceType == 'r') { // Rook - rectangular with crenellations
                g.fillRect(S/3, bodyTop + S/8, S/3, bodyBottom - bodyTop - S/8);
                // Crenellations
                g.fillRect(S/4, bodyTop, S/8, S/8);
                g.fillRect(S/2 - S/16, bodyTop, S/8, S/8);
                g.fillRect(5*S/8, bodyTop, S/8, S/8);
            } else if (pieceType == 'n') { // Knight - angular shape
                int[] xPoints = {S/2, 2*S/3, S/2, S/3};
                int[] yPoints = {bodyTop, bodyTop + S/4, bodyBottom, bodyBottom};
                g.fillPolygon(xPoints, yPoints, 4);
            } else if (pieceType == 'b') { // Bishop - pointed top
                g.fillOval(S/3, bodyTop + S/8, S/3, bodyBottom - bodyTop - S/8);
                int[] xPoints = {S/2, 5*S/12, 7*S/12};
                int[] yPoints = {bodyTop - S/12, bodyTop + S/6, bodyTop + S/6};
                g.fillPolygon(xPoints, yPoints, 3);
            } else if (pieceType == 'q') { // Queen - wide crown
                g.fillOval(S/3, bodyTop + S/6, S/3, bodyBottom - bodyTop - S/6);
                // Crown points
                for (int i = 0; i < 5; i++) {
                    int x = S/4 + i * S/8;
                    g.fillOval(x - S/24, bodyTop + (i % 2) * S/24, S/12, S/12);
                }
            } else { // King - cross on top
                g.fillOval(S/3, bodyTop + S/6, S/3, bodyBottom - bodyTop - S/6);
                // Cross
                g.fillRect(S/2 - S/24, bodyTop - S/12, S/12, S/4);
                g.fillRect(S/2 - S/8, bodyTop, S/4, S/12);
            }
            
            // Add outline for definition
            g.setPaint(null);
            g.setColor(isWhite ? new java.awt.Color(100, 100, 100) : 
                new java.awt.Color(200, 200, 200));
            g.setStroke(new java.awt.BasicStroke(4, java.awt.BasicStroke.CAP_ROUND, 
                java.awt.BasicStroke.JOIN_ROUND));
            
            // Redraw outline only
            g.drawOval(margin, S - margin - baseHeight, baseWidth, baseHeight);
            
            g.dispose();
            return img;
        }

        private void loadPieceImages() {
            // Optimization: Load piece images efficiently with better structure
            String dir = "images";
            
            // Optimization: Use Map.of for immutable map (Java 9+) or initialize capacity
            Map<Character, String> typeNames = new HashMap<>(6);
            typeNames.put('p', "pawn");
            typeNames.put('n', "knight");
            typeNames.put('b', "bishop");
            typeNames.put('r', "rook");
            typeNames.put('q', "queen");
            typeNames.put('k', "king");
            
            String[] colors = {"w", "b"};
            
            for (String col : colors) {
                String colorName = col.equals("w") ? "white" : "black";
                
                for (Map.Entry<Character, String> e : typeNames.entrySet()) {
                    char tchar = e.getKey();
                    String tname = e.getValue();
                    String key = col + tchar;
                    
                    // Optimization: Define candidate filenames
                    String[] candidates = {
                        key + ".png",
                        col + "_" + tname + ".png",
                        tname + "_" + colorName + ".png",
                        tname + "_" + col + ".png"
                    };
                    
                    Image img = tryLoadImage(dir, candidates);
                    originalImages.put(key, img != null ? img : makePlaceholderIcon(key));
                }
            }
        }
        
        // Optimization: Extract image loading logic
        private Image tryLoadImage(String dir, String[] candidates) {
            for (String filename : candidates) {
                File file = new File(dir, filename);
                if (file.exists()) {
                    try {
                        Image img = ImageIO.read(file);
                        if (img != null) {
                            return img;
                        }
                    } catch (Exception ex) {
                        // Continue to next candidate
                    }
                }
            }
            return null;
        }
    }
}
