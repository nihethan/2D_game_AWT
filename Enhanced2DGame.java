package AWT;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

public class Enhanced2DGame extends Canvas implements Runnable {
    private int playerX = 50, playerY = 50, playerSize = 30;
    private int score = 0;
    private boolean running = false;
    private boolean paused = false; // Flag for pausing
    private Thread gameThread;
    private ArrayList<Point> coins = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private Random random = new Random();
    private final int FPS = 60;

    private long lastCoinCollectionTime = 0;
    private long coinCollectionCooldown = 200; // 200 ms cooldown between coin collections

    public Enhanced2DGame() {
        JFrame frame = new JFrame("Enhanced 2D Game");
        Canvas canvas = this;
        canvas.setSize(800, 600);
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Generate initial coins and enemies
        for (int i = 0; i < 9; i++) {
            coins.add(new Point(random.nextInt(750), random.nextInt(550)));
        }

        // Create enemies with flexible movement types and reduced speeds
        for (int i = 0; i < 5; i++) { // Increased number of enemies
            enemies.add(new Enemy(random.nextInt(750), random.nextInt(550), randomMovement(), randomSpeed(), randomPattern()));
        }

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP) playerY -= 10;
                if (key == KeyEvent.VK_DOWN) playerY += 10;
                if (key == KeyEvent.VK_LEFT) playerX -= 10;
                if (key == KeyEvent.VK_RIGHT) playerX += 10;

                // Toggle pause with 'P' key
                if (key == KeyEvent.VK_P) {
                    paused = !paused;
                }
            }
        });

        this.setFocusable(true);
        startGame();
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGame() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / FPS;

        while (running) {
            long now = System.nanoTime();
            if (now - lastTime >= nsPerFrame) {
                updateGame();
                repaint();
                lastTime = now;
            }

            // Sleep briefly to reduce CPU usage
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateGame() {
        // Check coin collection with cooldown effect
        if (System.currentTimeMillis() - lastCoinCollectionTime > coinCollectionCooldown) {
            coins.removeIf(coin -> {
                if (Math.abs(playerX - coin.x) < playerSize && Math.abs(playerY - coin.y) < playerSize) {
                    score += 10;
                    lastCoinCollectionTime = System.currentTimeMillis();
                    return true;
                }
                return false;
            });
        }

        // Check if all coins are collected
        if (coins.isEmpty()) {
            running = false;
            JOptionPane.showMessageDialog(null, "You Won! Final Score: " + score, "Victory", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }

        // Move enemies with different behaviors (only if not paused)
        if (!paused) {
            for (Enemy enemy : enemies) {
                enemy.move(playerX, playerY);
            }
        }

        // Check collision with enemies
        for (Enemy enemy : enemies) {
            if (Math.abs(playerX - enemy.x) < playerSize && Math.abs(playerY - enemy.y) < playerSize) {
                running = false;
                JOptionPane.showMessageDialog(null, "Game Over! Final Score: " + score, "Game Over", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        // Clear the screen with gradient
        GradientPaint gradient = new GradientPaint(0, 0, Color.CYAN, getWidth(), getHeight(), Color.GREEN, true);
        ((Graphics2D) g).setPaint(gradient);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw player with a simple animation trail effect
        g.setColor(new Color(0, 0, 255, 128)); // Semi-transparent blue
        g.fillOval(playerX, playerY, playerSize, playerSize);

        // Draw coins with some glowing effect
        g.setColor(Color.YELLOW);
        for (Point coin : coins) {
            g.fillOval(coin.x, coin.y, 15, 15);
        }

        // Draw enemies with fading effect
        for (Enemy enemy : enemies) {
            g.setColor(new Color(255, 0, 0, 180)); // Red with transparency
            g.fillRect(enemy.x, enemy.y, playerSize, playerSize);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 20);

        // Draw pause state
        if (paused) {
            g.setColor(new Color(255, 255, 255, 128)); // Semi-transparent white for the pause overlay
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("PAUSED", getWidth() / 2 - 100, getHeight() / 2);
        }

        // Synchronize graphics for smoother rendering
        Toolkit.getDefaultToolkit().sync();
    }

    public static void main(String[] args) {
        new Enhanced2DGame();
    }

    // Random movement types for enemies
    private MovementType randomMovement() {
        int type = random.nextInt(4); // Randomize between 4 types
        switch (type) {
            case 0:
                return MovementType.RANDOM;
            case 1:
                return MovementType.CHASE;
            case 2:
                return MovementType.PATTERNED;
            default:
                return MovementType.ZIGZAG; // New movement type
        }
    }

    // Random speed for enemies (slower speed range)
    private int randomSpeed() {
        return random.nextInt(2) + 1; // Speeds 1 or 2
    }

    // Random pattern for enemies
    private String randomPattern() {
        String[] patterns = {"Sinusoidal", "Zigzag", "Straight"};
        return patterns[random.nextInt(patterns.length)];
    }

    // Enum for movement types
    enum MovementType {
        RANDOM,
        CHASE,
        PATTERNED,
        ZIGZAG
    }

    // Enemy class with multiple movement behaviors
    class Enemy {
        int x, y, dx, dy, speed;
        MovementType movementType;
        String pattern;

        Enemy(int x, int y, MovementType movementType, int speed, String pattern) {
            this.x = x;
            this.y = y;
            this.dx = random.nextInt(3) - 1; // Initial random movement
            this.dy = random.nextInt(3) - 1; // Initial random movement
            this.movementType = movementType;
            this.speed = speed;
            this.pattern = pattern;
        }

        void move(int playerX, int playerY) {
            switch (movementType) {
                case RANDOM:
                    // Random movement
                    x += random.nextInt(3) - 1;
                    y += random.nextInt(3) - 1;
                    break;
                case CHASE:
                    // Chasing the player (simple AI)
                    if (x < playerX) x += speed;
                    if (x > playerX) x -= speed;
                    if (y < playerY) y += speed;
                    if (y > playerY) y -= speed;
                    break;
                case PATTERNED:
                    // Moving in a specific pattern (e.g., sinusoidal wave)
                    if (pattern.equals("Sinusoidal")) {
                        x += dx;
                        y += (int) (Math.sin(System.nanoTime() * 0.0001) * 5); // Sinusoidal movement
                    } else if (pattern.equals("Zigzag")) {
                        x += speed;
                        y += (int) (Math.sin(System.nanoTime() * 0.00005) * 10); // Zigzag pattern
                    }
                    break;
                case ZIGZAG:
                    // Zigzag movement
                    x += speed;
                    y += (int) (Math.sin(System.nanoTime() * 0.00005) * 15); // Zigzag movement with different amplitude
                    break;
            }

            // Boundaries check for screen wrapping
            if (x < 0) x = 770;
            if (x > 770) x = 0;
            if (y < 0) y = 570;
            if (y > 570) y = 0;
        }
    }
}
