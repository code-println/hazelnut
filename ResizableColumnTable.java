import javax.swing.*;
import javax.swing.plaf.LayerUI;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;

public class ResizableColumnTable {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Resizable Column Table");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Создаем таблицу с данными
            String[] columns = {"Column 1", "Column 2", "Column 3"};
            Object[][] data = {
                    {"1", "2", "3"},
                    {"4", "5", "6"},
                    {"7", "8", "9"}
            };
            JTable table = new JTable(data, columns);

            // Создаем кастомный LayerUI
            ResizableColumnLayerUI layerUI = new ResizableColumnLayerUI(table);

            // Создаем JLayer с таблицей и кастомным LayerUI
            JLayer<JTable> jLayer = new JLayer<>(table, layerUI);

            frame.add(new JScrollPane(jLayer));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class ResizableColumnLayerUI extends LayerUI<JTable> {
        private final JTable table;
        private final JTableHeader header;
        private int columnToResize = -1; // Индекс колонки, которую изменяем
        private int xOffset = 0; // Позиция линии относительно таблицы
        private boolean resizing = false; // Флаг, указывающий, что идет изменение размера
        private Cursor defaultCursor; // Сохраняем стандартный курсор

        public ResizableColumnLayerUI(JTable table) {
            this.table = table;
            this.header = table.getTableHeader(); // Получаем заголовок таблицы
            defaultCursor = table.getCursor(); // Сохраняем стандартный курсор таблицы
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        public void uninstallUI(JComponent c) {
            JLayer jlayer = (JLayer) c;
            jlayer.setLayerEventMask(0);
            super.uninstallUI(c);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (resizing) {
                // Отрисовываем вертикальную линию
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.RED);
                g2.drawLine(xOffset, 0, xOffset, table.getHeight());
                g2.dispose();
            }
        }

        @Override
        protected void processMouseEvent(MouseEvent e, JLayer<? extends JTable> l) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1) {
                // Проверяем, находится ли курсор на границе колонки (в таблице или заголовке)
                int column = getColumnAtPoint(e.getPoint(), e.getSource() == header);
                if (column != -1) {
                    columnToResize = column;
                    xOffset = e.getX(); // Запоминаем начальную позицию линии
                    resizing = true; // Включаем режим изменения размера
                    l.repaint(); // Перерисовываем JLayer
                }
            } else if (e.getID() == MouseEvent.MOUSE_RELEASED && e.getButton() == MouseEvent.BUTTON1) {
                if (resizing) {
                    // Когда кнопка мыши отпущена, изменяем ширину колонки
                    int newWidth = xOffset - table.getCellRect(0, columnToResize, true).x;
                    table.getColumnModel().getColumn(columnToResize).setPreferredWidth(newWidth);
                    resizing = false; // Завершаем режим изменения размера
                    columnToResize = -1; // Сбрасываем индекс колонки
                    l.repaint(); // Перерисовываем JLayer
                }
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends JTable> l) {
            if (resizing) {
                // Обновляем позицию вертикальной линии
                xOffset = e.getX();
                l.repaint(); // Перерисовываем JLayer
            } else {
                // Изменяем курсор, если мышь находится на границе колонки (в таблице или заголовке)
                int column = getColumnAtPoint(e.getPoint(), e.getSource() == header);
                if (column != -1) {
                    l.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)); // Устанавливаем курсор "ресайз"
                } else {
                    l.setCursor(defaultCursor); // Возвращаем стандартный курсор
                }
            }
        }

        /**
         * Определяет, находится ли курсор на границе колонки или на одном пикселе влево/вправо от границы.
         *
         * @param p          Точка, в которой находится курсор.
         * @param isHeader    Флаг, указывающий, находится ли курсор в заголовке таблицы.
         * @return Индекс колонки, если курсор на границе или рядом, иначе -1.
         */
        private int getColumnAtPoint(Point p, boolean isHeader) {
            int column;
            if (isHeader) {
                // Если курсор в заголовке, используем header.columnAtPoint
                column = header.columnAtPoint(p);
            } else {
                // Иначе используем table.columnAtPoint
                column = table.columnAtPoint(p);
            }
            if (column != -1) {
                Rectangle rect;
                if (isHeader) {
                    // Получаем прямоугольник для колонки в заголовке
                    rect = header.getHeaderRect(column);
                } else {
                    // Получаем прямоугольник для колонки в таблице
                    rect = table.getCellRect(0, column, true);
                }
                int borderX = rect.x + rect.width; // X-координата границы колонки
                // Проверяем, находится ли курсор в пределах одного пикселя от границы
                if (Math.abs(p.x - borderX) <= 3) {
                    return column;
                }
            }
            return -1;
        }
    }
}
