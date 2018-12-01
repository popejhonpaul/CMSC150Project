import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.Object;
import java.text.DecimalFormat;

public class SystemUI{
    JFrame mainFrame = new JFrame("Select...");
    JPanel container = new JPanel(new FlowLayout());
    JButton polyRegBtn = new JButton("Polynomial Regression");
    JButton quadSplineBtn = new JButton("Quadratic Spline Interpolation");
    JButton optBtn = new JButton("Optimization");
    JTable inputTable;

    ArrayList<Point> pointList = new ArrayList<Point>();
    double[] resultingRHS;

    public SystemUI(){
        mainFrame.setPreferredSize(new Dimension(280, 140));
        polyRegBtn.setPreferredSize(new Dimension(250, 30));
        quadSplineBtn.setPreferredSize(new Dimension(250, 30));
        optBtn.setPreferredSize(new Dimension(250, 30));
        
        container.add(polyRegBtn);
        polyRegBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                JFrame polyRegFrame = new JFrame("Polynomial Regression");
                JPanel mainPolyRegPanel = new JPanel();
                mainPolyRegPanel.setPreferredSize(new Dimension(280, 310));
                mainPolyRegPanel.setLayout(new BoxLayout(mainPolyRegPanel, BoxLayout.PAGE_AXIS));
                JPanel polyRegPnl = new JPanel(new FlowLayout());

                optBtn.setEnabled(false);
                quadSplineBtn.setEnabled(false);

                JTextArea functionTextArea = new JTextArea();
                JTextField xTxtFld = new JTextField();
                xTxtFld.setPreferredSize(new Dimension(30, 20));
                JLabel estimateLabel = new JLabel();
                estimateLabel.setVisible(false);
                /*JTextField estimateTxtFld = new JTextField();
                estimateTxtFld.setPreferredSize(new Dimension(160, 20));
                estimateTxtFld.setVisible(false);*/
                
                polyRegFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        pointList.clear();
                        optBtn.setEnabled(true);
                        quadSplineBtn.setEnabled(true);
                        //System.out.println("Exited polynomialRegression");
                    }
                });

                JTextField degreeTxtFld = new JTextField();
                JPanel degreeContainer = new JPanel();

                JButton openFileBtn = new JButton("Browse...");
                openFileBtn.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        pointList = readFile();
                        HashMap<Double, Double> inputMap = new HashMap<Double, Double>();

                        for(int i = 0; i < pointList.size(); i++)
                            inputMap.put(pointList.get(i).x, pointList.get(i).y);
                        
                        inputTable.setModel(toTableModel(inputMap));
                    }
                });
                polyRegPnl.add(new JLabel("Select input file"));
                polyRegPnl.add(openFileBtn);
                
                polyRegPnl.add(new JLabel("Degree: "));
                degreeTxtFld.setPreferredSize(new Dimension(30, 20));
                polyRegPnl.add(degreeTxtFld);
                
                JButton computeBtn = new JButton("Compute");
                computeBtn.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if(degreeTxtFld.getText().equals("") || pointList.isEmpty()){
                            JOptionPane.showMessageDialog(polyRegFrame, "Enter degree or select input file.");
                        }else if(!degreeTxtFld.getText().matches("^\\d*$")){
                            JOptionPane.showMessageDialog(polyRegFrame, "Degree should be an integer.");
                        }else{
                            int degree = Integer.parseInt(degreeTxtFld.getText());
                            double[][] matrix = new double[(degree+1)][(degree+2)];                   //matrix

                            for(int i = 0; i < degree+1; i++){
                                for(int j = 0; j < degree+2; j++){
                                    double sum = 0;
                                    if(j == degree+1){
                                        for(int k = 0; k < pointList.size(); k++)
                                            sum = sum + (Math.pow(pointList.get(k).x, i)*pointList.get(k).y);
                                    }else{
                                        for(int k = 0; k < pointList.size(); k++)
                                            sum = sum + (Math.pow(pointList.get(k).x, (i+j)));
                                    }
                                    matrix[i][j] = sum;
                                }
                            }
                            
                            for(int i = 0; i < degree+1; i++){                          //printMAtrix
                                for(int j = 0; j < degree+2; j++){
                                    System.out.print("\t" +matrix[i][j]);
                                }
                                System.out.println();
                            }
                            resultingRHS = gaussJordanElim(matrix, (degree+1), (degree+2));
                            
                            String functionText = "function (x) ";
                            for(int i = 0; i < (degree+1); i++){
                                functionText = functionText + resultingRHS[i];
                                if(i != 0) functionText = functionText + " * x^" + i;
                                if(i != degree) functionText = functionText + " + ";
                            }

                            functionTextArea.setText(functionText);
                        }
                    }
                });
                polyRegPnl.add(computeBtn);

                JPanel polyRegBtmPanel = new JPanel();
                polyRegBtmPanel.setPreferredSize(new Dimension(280, 215));

                JPanel leftPanel = new JPanel();
                leftPanel.setPreferredSize(new Dimension(100, 215));
                inputTable = new JTable(toTableModel(new HashMap<Integer, Integer>()));
                JScrollPane inputTableScrlPn = new JScrollPane(inputTable);
                inputTableScrlPn.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                inputTableScrlPn.setPreferredSize(new Dimension(80, 210));
                leftPanel.add(inputTableScrlPn);
                polyRegBtmPanel.add(leftPanel, BorderLayout.LINE_START);

                JPanel rightPanel = new JPanel();
                rightPanel.setPreferredSize(new Dimension(160, 215));
                functionTextArea.setEditable(false);
                functionTextArea.setLineWrap(true);
                JScrollPane functionTextAreaScrollPane = new JScrollPane(functionTextArea);
                functionTextAreaScrollPane.setPreferredSize(new Dimension(150, 100));
                rightPanel.add(functionTextAreaScrollPane);
                
                rightPanel.add(new JLabel("x: "));
                rightPanel.add(xTxtFld);

                JButton findEstimateBtn = new JButton("Find Estimate");
                findEstimateBtn.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if(xTxtFld.getText().equals("")){
                            JOptionPane.showMessageDialog(polyRegFrame, "Enter x.");
                        }else if(degreeTxtFld.getText().equals("") || pointList.isEmpty()){
                            JOptionPane.showMessageDialog(polyRegFrame, "Enter degree or select input file.");
                        }else if(!xTxtFld.getText().matches("^\\d*\\.?\\d+|\\d+\\.\\d*$")){
                            JOptionPane.showMessageDialog(polyRegFrame, "x should be a number.");
                        }else{
                            int degree = Integer.parseInt(degreeTxtFld.getText());
                            DecimalFormat df = new DecimalFormat("#.000000");
                            estimateLabel.setText("f(" + xTxtFld.getText() + ") = " + df.format(evalFxtion(Double.valueOf(xTxtFld.getText()), resultingRHS, degree+1)));
                            estimateLabel.setVisible(true);
                        }
                    }
                });

                rightPanel.add(findEstimateBtn);
                rightPanel.add(estimateLabel);
                //rightPanel.add(estimateTxtFld);
                polyRegBtmPanel.add(rightPanel, BorderLayout.LINE_END);
                
                mainPolyRegPanel.add(polyRegPnl);
                mainPolyRegPanel.add(polyRegBtmPanel);
                polyRegFrame.add(mainPolyRegPanel);
                polyRegFrame.pack();
                polyRegFrame.setResizable(false);
                polyRegFrame.setVisible(true);
            }
        });
        container.add(quadSplineBtn);
        quadSplineBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                JFrame quadSplineFrame = new JFrame("Quadratic Spline Interpolation");
                JPanel quadSplinePnl = new JPanel(new FlowLayout());
                quadSplineFrame.setPreferredSize(new Dimension(280, 330));
                JTextArea functionTextArea = new JTextArea();

                polyRegBtn.setEnabled(false);
                optBtn.setEnabled(false);

                quadSplineFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        polyRegBtn.setEnabled(true);
                        optBtn.setEnabled(true);
                        pointList.clear();
                    }
                });

                JButton openFileBtn = new JButton("Browse...");
                openFileBtn.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        pointList = readFile();
                        int counter = 1;
                        int matrixSize = 3*(pointList.size()-1);
                        double[][] matrix = new double[matrixSize][matrixSize+1];

                        //condition 2
                        matrix[0][0] = Math.pow(pointList.get(0).x, 2);
                        matrix[0][1] = pointList.get(0).x;
                        matrix[0][2] = 1;
                        matrix[0][matrixSize] = pointList.get(0).y;

                        matrix[matrixSize-1][matrixSize-3] = Math.pow(pointList.get(pointList.size()-1).x, 2);
                        matrix[matrixSize-1][matrixSize-2] = pointList.get(pointList.size()-1).x;
                        matrix[matrixSize-1][matrixSize-1] = 1;
                        matrix[matrixSize-1][matrixSize] = pointList.get(pointList.size()-1).y;

                        //condition 3
                        for(int i = 2; i < pointList.size(); i++){
                            matrix[counter][(3*(i-2))+0] = 2*pointList.get(i-1).x;
                            matrix[counter][(3*(i-2))+1] = 1;
                            matrix[counter][(3*(i-1))+0] = -(2*pointList.get(i-1).x);
                            matrix[counter][(3*(i-1))+1] = -1;
                            counter++;
                        }

                        //condition 4
                        matrix[counter][0] = 1;
                        counter++; 

                        //condition 1
                        for(int i = 1; i < pointList.size()-1; i++){
                            matrix[counter+(i-1)][(3*(i-1))+0] = Math.pow(pointList.get(i).x, 2);
                            matrix[counter+(i-1)][(3*(i-1))+1] = pointList.get(i).x;
                            matrix[counter+(i-1)][(3*(i-1))+2] = 1;
                            matrix[counter+(i-1)][matrixSize] = pointList.get(i).y;

                            matrix[counter+(i)][(3*(i))+0] = Math.pow(pointList.get(i).x, 2);
                            matrix[counter+(i)][(3*(i))+1] = pointList.get(i).x;
                            matrix[counter+(i)][(3*(i))+2] = 1;
                            matrix[counter+(i)][matrixSize] = pointList.get(i).y;
                            counter++;
                        }

                        resultingRHS = gaussJordanElim(matrix, matrixSize, matrixSize+1);

                        String tempString2 = "";
                        for(int i = 0; i < pointList.size()-1; i++){
                            String tempString = pointList.get(i).x + " <= x <= " + pointList.get(i+1).x + "\nfunction (x) ";
                            for(int j = 0; j < 3; j++){
                                if(resultingRHS[(3*i+j)] != 0){
                                    tempString = tempString + resultingRHS[(3*i)+j];
                                    if(j != 2) tempString = tempString + " * x^" + (2-j);
                                    if(j != 2) tempString = tempString + " + ";
                                }
                            }
                            tempString = tempString + "\n\n";
                            tempString2 = tempString2 + tempString;
                        }
                        functionTextArea.setText(tempString2);

                        for(int i = 0; i < matrixSize; i++){
                            for(int j = 0; j < matrixSize+1; j++){
                                System.out.print("\t" +matrix[i][j]);
                            }
                            System.out.println();
                        }
                    }
                });

                quadSplinePnl.add(new JLabel("Select input file"));
                quadSplinePnl.add(openFileBtn);

                functionTextArea.setEditable(false);
                functionTextArea.setLineWrap(true);
                JScrollPane functionTextAreaScrollPane = new JScrollPane(functionTextArea);
                functionTextAreaScrollPane.setPreferredSize(new Dimension(260, 200));
                quadSplinePnl.add(functionTextAreaScrollPane);

                JLabel estimateLabel = new JLabel();
                estimateLabel.setVisible(false);
                quadSplinePnl.add(new JLabel("x: "));
                JTextField xTxtFld = new JTextField();
                xTxtFld.setPreferredSize(new Dimension(50, 20));
                JButton findEstimateBtn = new JButton("Find Estimate");
                findEstimateBtn.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if(xTxtFld.getText().equals("")){
                            JOptionPane.showMessageDialog(quadSplineFrame, "Enter x.");
                        }else if(pointList.isEmpty()){
                            JOptionPane.showMessageDialog(quadSplineFrame, "Select input file.");
                        }else if(!xTxtFld.getText().matches("^\\d*\\.?\\d+|\\d+\\.\\d*$")){
                            JOptionPane.showMessageDialog(quadSplineFrame, "x should be a number.");
                        }else{
                            double xValue = Double.valueOf(xTxtFld.getText());
                            double[] tempArray = new double[3];
                            DecimalFormat df = new DecimalFormat("#.000000");

                            for(int i = 0; i < pointList.size()-1; i++){
                                if(pointList.get(i).x <= xValue && xValue <= pointList.get(i+1).x){
                                    for(int j = 0; j < 3; j++){
                                        tempArray[2-j] = resultingRHS[(3*i)+j];
                                    }
                                }
                            }
                            estimateLabel.setText("function ("+ xTxtFld.getText()+ ") = " + df.format(evalFxtion(Double.valueOf(xTxtFld.getText()), tempArray, 3)));
                            estimateLabel.setVisible(true);
                        }
                    }
                });
                quadSplinePnl.add(xTxtFld);
                quadSplinePnl.add(findEstimateBtn);
                quadSplinePnl.add(estimateLabel);

                quadSplineFrame.add(quadSplinePnl);
                quadSplineFrame.pack();
                quadSplineFrame.setResizable(false);
                quadSplineFrame.setVisible(true);
            }
        });
        container.add(optBtn);
        optBtn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                JFrame optFrame = new JFrame("Optimization");
                optFrame.setPreferredSize(new Dimension(280, 330));

                polyRegBtn.setEnabled(false);
                quadSplineBtn.setEnabled(false);

                optFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        pointList.clear();
                        polyRegBtn.setEnabled(true);
                        quadSplineBtn.setEnabled(true);
                        //System.out.println("Exited polynomialRegression");
                    }
                });

                optFrame.pack();
                optFrame.setResizable(false);
                optFrame.setVisible(true);
            }
        });

        mainFrame.add(container);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }
    public double evalFxtion(double estimateVal, double[] resultingRHS, int nrow){
        double answer = 0;
        for(int i = 0; i < nrow; i++){
            answer = answer + (resultingRHS[i])*(Math.pow(estimateVal, i));
        }
        return(answer);
    }
    public double[][] switchRow(double[][] matrix, int row1, int row2, int ncol){
        for(int j = 0; j < ncol; j++){
            double tempValue = matrix[row1][j];
            matrix[row1][j] = matrix[row2][j];
            matrix[row2][j] = tempValue;
        }
        return(matrix);
    }
    public double[] gaussJordanElim(double[][] matrix, int nrow, int ncol){
        double[][] resultingMatrix = matrix;
        DecimalFormat df = new DecimalFormat("#.000000");

        for(int i = 0; i < nrow; i++){
            if(i != nrow-1){
                int max = i;                                            //for pivoting
                for(int k = i+1; k < nrow; k++){
                    if(Math.abs(resultingMatrix[max][i]) < Math.abs(resultingMatrix[k][i])) max = k;
                }
                resultingMatrix = switchRow(resultingMatrix, max, i, ncol);
            }
            double pivotELement = resultingMatrix[i][i];
            for(int j = 0; j < ncol; j++){
                resultingMatrix[i][j] = resultingMatrix[i][j]/pivotELement;
            }

            for(int j = 0; j < nrow; j++){
                double vtbe = resultingMatrix[j][i];
                for(int k = 0; k < ncol; k++){
                    if(i != j){
                        double tempValue2 = resultingMatrix[i][k]*vtbe;
                        resultingMatrix[j][k] = resultingMatrix[j][k]-tempValue2;
                    }
                }
            }
        }
        /*for(int i = 0; i < nrow; i++){                          //printMAtrix
            for(int j = 0; j < ncol; j++){
                System.out.print("\t" +resultingMatrix[i][j]);
            }
            System.out.println();
        }*/

        double[] resultingRHS = new double[nrow];
        for(int i = 0; i < nrow; i++) resultingRHS[i] = Double.valueOf(df.format(resultingMatrix[i][ncol-1]));
        return(resultingRHS);
    }
    public static TableModel toTableModel(Map map) {                            //this will create the table containing the words and their corresponding frequency
        DefaultTableModel model = new DefaultTableModel(new Object[]{"x", "y"}, 0){
            @Override
            public boolean isCellEditable(int row, int column) {                //this will make the table uneditable
               return false;
            }
        };
        for (Iterator it = map.entrySet().iterator(); it.hasNext();){           //isa-isang ilalagay neto yung mga words with their respective frequency sa table
            Map.Entry entry = (Map.Entry)it.next();
            model.addRow(new Object[] { entry.getKey(), entry.getValue() });
        }
        return model;
    }
    public ArrayList<Point> readFile(){
        ArrayList<Point> pointList = new ArrayList<Point>();
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home")+"/Desktop");

        if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            try{
                Scanner input = new Scanner(file);
                while(input.hasNext()){
                    String temp = input.nextLine();
                    String[] tokens = temp.split(",");
                    Point tempPoint = new Point();
                    tempPoint.x = Double.parseDouble(tokens[0]);
                    tempPoint.y = Double.parseDouble(tokens[1]);
                    pointList.add(tempPoint);
                }
            }catch(FileNotFoundException e){
                JOptionPane.showMessageDialog(null, "I am happy.");
            }
        }
        return pointList;
    }
    public static void main(String args[]){
        new SystemUI();
    }
}
class Point{
    double x;
    double y;
}