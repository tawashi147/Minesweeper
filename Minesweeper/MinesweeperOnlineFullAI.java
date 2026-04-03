import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class MinesweeperOnlineFullAI extends JFrame {

    int ROWS=9, COLS=9, MINES=10;
    JButton[][] buttons;
    boolean[][] isMine, revealed, flagged;

    boolean gameOver=false, firstClick=true, darkMode=false;

    JLabel timerLabel, minesLabel, bestLabel;
    javax.swing.Timer timer;
    int seconds=0, flagsLeft;

    JPanel gridPanel;

    ImageIcon flagIcon, mineIcon;

    public MinesweeperOnlineFullAI(){
        setTitle("Minesweeper Online Full AI");
        setSize(600,700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadIcons();
        createTop();
        setDifficulty(9,9,10);

        setVisible(true);
    }

    void loadIcons(){
        try{
            flagIcon = new ImageIcon(ImageIO.read(new File("images/flag.png")));
            mineIcon = new ImageIcon(ImageIO.read(new File("images/mine.png")));
        }catch(Exception e){
            flagIcon = null;
            mineIcon = null;
        }
    }

    void createTop(){
        JPanel top=new JPanel();
        top.setLayout(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();
        c.insets=new Insets(2,2,2,2);

        JButton easy=new JButton("初級");
        JButton mid=new JButton("中級");
        JButton hard=new JButton("上級");
        JButton custom=new JButton("カスタム");
        JButton restart=new JButton("🔁");
        JButton hint=new JButton("🧠");
        JButton theme=new JButton("🌙");

        timerLabel=new JLabel("⏱0");
        minesLabel=new JLabel("💣0");
        bestLabel=new JLabel("🏆--");

        easy.addActionListener(e->setDifficulty(9,9,10));
        mid.addActionListener(e->setDifficulty(16,16,40));
        hard.addActionListener(e->setDifficulty(16,30,99));
        custom.addActionListener(e->customDifficulty());
        restart.addActionListener(e->reset());
        hint.addActionListener(e->fullAIHint());
        theme.addActionListener(e->{darkMode=!darkMode; applyTheme();});

        c.gridx=0; c.gridy=0; top.add(easy,c);
        c.gridx=1; top.add(mid,c);
        c.gridx=2; top.add(hard,c);
        c.gridx=3; top.add(custom,c);
        c.gridx=4; top.add(restart,c);
        c.gridx=5; top.add(hint,c);
        c.gridx=6; top.add(theme,c);
        c.gridx=0; c.gridy=1; c.gridwidth=2; top.add(timerLabel,c);
        c.gridx=2; c.gridwidth=2; top.add(minesLabel,c);
        c.gridx=4; c.gridwidth=2; top.add(bestLabel,c);

        add(top,BorderLayout.NORTH);
    }

    void setDifficulty(int r,int c,int m){
        ROWS=r; COLS=c; MINES=m;
        reset();
    }

    void customDifficulty(){
        String sRows=JOptionPane.showInputDialog(this,"行数(5-30):",ROWS);
        String sCols=JOptionPane.showInputDialog(this,"列数(5-50):",COLS);
        String sMines=JOptionPane.showInputDialog(this,"地雷数:",MINES);
        try{
            int r=Integer.parseInt(sRows), c=Integer.parseInt(sCols), m=Integer.parseInt(sMines);
            if(r>0&&c>0&&m>0) setDifficulty(r,c,m);
        }catch(Exception e){}
    }

    void reset(){
        if(gridPanel!=null) remove(gridPanel);

        buttons=new JButton[ROWS][COLS];
        isMine=new boolean[ROWS][COLS];
        revealed=new boolean[ROWS][COLS];
        flagged=new boolean[ROWS][COLS];

        flagsLeft=MINES;
        minesLabel.setText("💣"+flagsLeft);

        gameOver=false;
        firstClick=true;

        gridPanel=new JPanel(new GridLayout(ROWS,COLS));
        add(gridPanel,BorderLayout.CENTER);

        for(int r=0;r<ROWS;r++){
            for(int c=0;c<COLS;c++){
                int rr=r,cc=c;
                JButton btn=new JButton();
                btn.setFont(new Font("Arial",Font.BOLD,16));
                btn.setBackground(darkMode?Color.DARK_GRAY:Color.LIGHT_GRAY);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                btn.addMouseListener(new MouseAdapter(){
                    public void mousePressed(MouseEvent e){
                        if(gameOver)return;

                        if(SwingUtilities.isRightMouseButton(e)){
                            toggleFlag(rr,cc);
                        }else{
                            reveal(rr,cc);
                        }
                    }
                });

                buttons[r][c]=btn;
                gridPanel.add(btn);
            }
        }

        revalidate();
        repaint();

        startTimer();
        loadBest();
    }

    void startTimer(){
        if(timer!=null)timer.stop();
        seconds=0;
        timerLabel.setText("⏱0");
        timer=new javax.swing.Timer(1000,e->{seconds++; timerLabel.setText("⏱"+seconds);});
    }

    void placeMines(int sr,int sc){
        Random rand=new Random();
        int placed=0;
        while(placed<MINES){
            int r=rand.nextInt(ROWS), c=rand.nextInt(COLS);
            if(!isMine[r][c]&&(r!=sr||c!=sc)){
                isMine[r][c]=true;
                placed++;
            }
        }
    }

    void toggleFlag(int r,int c){
        if(revealed[r][c])return;
        flagged[r][c]=!flagged[r][c];
        buttons[r][c].setIcon(flagged[r][c]?flagIcon:null);
        flagsLeft += flagged[r][c]?-1:1;
        minesLabel.setText("💣"+flagsLeft);
    }

    void reveal(int r,int c){
        if(r<0||c<0||r>=ROWS||c>=COLS)return;
        if(revealed[r][c]||flagged[r][c])return;

        if(firstClick){
            placeMines(r,c);
            firstClick=false;
            timer.start();
        }

        revealed[r][c]=true;
        JButton btn = buttons[r][c];
        btn.setEnabled(false);
        btn.setBackground(darkMode?Color.GRAY:Color.WHITE);

        if(isMine[r][c]){
            btn.setIcon(mineIcon);
            gameOver(false);
            return;
        }

        int count=countMines(r,c);
        if(count>0){
            btn.setText(""+count);
            btn.setForeground(getColor(count));
        }else{
            for(int dr=-1;dr<=1;dr++)
                for(int dc=-1;dc<=1;dc++)
                    reveal(r+dr,c+dc);
        }

        checkWin();
    }

    int countMines(int r,int c){
        int cnt=0;
        for(int dr=-1;dr<=1;dr++)
            for(int dc=-1;dc<=1;dc++){
                int nr=r+dr,nc=c+dc;
                if(nr>=0&&nc>=0&&nr<ROWS&&nc<COLS)
                    if(isMine[nr][nc]) cnt++;
            }
        return cnt;
    }

    Color getColor(int n){
        return switch(n){
            case 1->Color.BLUE;
            case 2->Color.GREEN;
            case 3->Color.RED;
            default->Color.BLACK;
        };
    }

    void fullAIHint(){
        java.util.List<Point> frontier=new ArrayList<>();
        for(int r=0;r<ROWS;r++)
            for(int c=0;c<COLS;c++)
                if(revealed[r][c])
                    for(int dr=-1;dr<=1;dr++)
                        for(int dc=-1;dc<=1;dc++){
                            int nr=r+dr,nc=c+dc;
                            if(nr>=0&&nc>=0&&nr<ROWS&&nc<COLS)
                                if(!revealed[nr][nc]&&!flagged[nr][nc]){
                                    Point p=new Point(nr,nc);
                                    if(!frontier.contains(p))frontier.add(p);
                                }
                        }

        int n=frontier.size();
        if(n==0)return;

        int total=0;
        int[] mineCount=new int[n];

        int limit=1<<Math.min(n,15);

        for(int mask=0;mask<limit;mask++){
            boolean valid=true;
            for(int r=0;r<ROWS;r++)
                for(int c=0;c<COLS;c++)
                    if(revealed[r][c]){
                        int need=countMines(r,c),flags=0;
                        for(int dr=-1;dr<=1;dr++)
                            for(int dc=-1;dc<=1;dc++){
                                int nr=r+dr,nc=c+dc;
                                if(nr>=0&&nc>=0&&nr<ROWS&&nc<COLS){
                                    if(flagged[nr][nc])flags++;
                                    else{
                                        for(int i=0;i<n;i++){
                                            Point p=frontier.get(i);
                                            if(p.x==nr&&p.y==nc)
                                                if((mask&(1<<i))!=0)flags++;
                                        }
                                    }
                                }
                            }
                        if(flags!=need) valid=false;
                    }
            if(!valid) continue;

            total++;
            for(int i=0;i<n;i++)
                if((mask&(1<<i))!=0) mineCount[i]++;
        }

        if(total==0)return;

        double best=1;
        Point bestP=null;
        for(int i=0;i<n;i++){
            double prob=(double)mineCount[i]/total;
            if(prob<best){
                best=prob;
                bestP=frontier.get(i);
            }
        }

        if(bestP!=null){
            final int bestR=bestP.x;
            final int bestC=bestP.y;
            buttons[bestR][bestC].setBackground(Color.CYAN);
            // 0.5秒で戻す
            new javax.swing.Timer(500, e -> {
                buttons[bestR][bestC].setBackground(darkMode?Color.DARK_GRAY:Color.LIGHT_GRAY);
                ((javax.swing.Timer)e.getSource()).stop();
            }).start();
        }
    }

    void gameOver(boolean win){
        gameOver=true;
        timer.stop();

        for(int r=0;r<ROWS;r++)
            for(int c=0;c<COLS;c++){
                buttons[r][c].setEnabled(false);
                if(isMine[r][c] && !revealed[r][c]) buttons[r][c].setIcon(mineIcon);
            }

        if(win) saveBest();

        JOptionPane.showMessageDialog(this, win?"クリア！":"ゲームオーバー");
    }

    void checkWin(){
        int safe=ROWS*COLS-MINES, open=0;
        for(int r=0;r<ROWS;r++)
            for(int c=0;c<COLS;c++)
                if(revealed[r][c]&&!isMine[r][c]) open++;
        if(open==safe) gameOver(true);
    }

    void saveBest(){
        try(PrintWriter out=new PrintWriter("score.txt")){
            out.println(seconds);
        }catch(Exception e){}
    }

    void loadBest(){
        try(BufferedReader br=new BufferedReader(new FileReader("score.txt"))){
            bestLabel.setText("🏆"+br.readLine());
        }catch(Exception e){
            bestLabel.setText("🏆--");
        }
    }

    void applyTheme(){
        for(int r=0;r<ROWS;r++)
            for(int c=0;c<COLS;c++){
                if(!revealed[r][c])
                    buttons[r][c].setBackground(darkMode?Color.DARK_GRAY:Color.LIGHT_GRAY);
            }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(MinesweeperOnlineFullAI::new);
    }
}