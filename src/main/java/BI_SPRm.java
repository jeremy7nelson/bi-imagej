import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.*;
import java.io.*;

public class BI_SPRm implements PlugInFilter
{
    private ImagePlus image;

    public int setup(String arg, ImagePlus imp)
    {
        image = imp;
        return DOES_ALL;
    }

    public void run(ImageProcessor unused)
    {
        // Assume 14 frames / sec
    	int frameRate = 14;

        ImageStack stack = image.getStack();
        int maxSlice = stack.getSize();
        int options = ImageStatistics.MEAN;
        Calibration calibration = image.getCalibration();

        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null)
        {
            new RoiManager();
            return;
        }
        int[] selectedIndexes = roiManager.getSelectedIndexes();
        if (selectedIndexes.length == 0)
        {
            IJ.showStatus("No regions selected");
            return;
        }

        PrintWriter file;

        try
        {
            file = new PrintWriter(image.getTitle() + ".bi");
        }
        catch (FileNotFoundException e)
        {
            IJ.showStatus("Unable to open output file");
            return;
        }

        file.write("Biosensing Instrument Build\t0\r\n");
        file.write(String.format("Rate\t%d\r\n", frameRate));
        file.write(String.format("DataPoints\t%d\r\n", maxSlice));
        file.write(String.format("RoiUnit\t%%\r\n"));
        file.write(String.format("TimeUnit\tsec\r\n"));
        file.write("FlowInjection\r\n");
        file.write("Shown\tTRUE\r\n");
        for (int index : selectedIndexes)
            file.write(String.format("Input\tRoi%d\r\n", index + 1));
        file.write("Data\r\n");
        file.write("Time (s)");
        for (int index : selectedIndexes)
            file.write(String.format("\tRoi%d (%%)", index + 1));
        file.write("\r\n");

        // slice numbers start with 1 for historical reasons
        for (int i = 1; i <= maxSlice; i++)
        {
            IJ.showProgress(i, maxSlice);
            file.write(String.format("%.3f", (double)(i - 1) / (double)frameRate));
            ImageProcessor processor = stack.getProcessor(i);
            for (int index : selectedIndexes)
            {
                processor.setRoi(roiManager.getRoi(index));
                ImageStatistics stats = ImageStatistics.getStatistics(processor, options, calibration);
                file.write(String.format("\t%.3f", stats.mean / 65536.0 * 100.0));
            }
            file.write("\r\n");
        }

        file.close();
    }
}
