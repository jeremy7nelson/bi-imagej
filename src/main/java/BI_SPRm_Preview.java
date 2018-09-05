import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.*;
import java.io.*;

public class BI_SPRm_Preview implements PlugInFilter
{
    private ImagePlus image;

    public int setup(String arg, ImagePlus imp)
    {
        image = imp;
        return DOES_16;
    }

    public void run(ImageProcessor unused)
    {
        // Assume 14 frames / sec
    	// Only process every 14th frame
    	int frameRate = 1;

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
            IJ.showStatus("Unable to open output.bi");
            return;
        }

        file.write("Biosensing Instrument Build\t131072\r\n");
        file.write(String.format("Rate\t%d\r\n", frameRate));
        // DataPoints can be larger than actual but not smaller
        file.write(String.format("DataPoints\t%d\r\n", (maxSlice / 14) + 1));
        file.write("FlowInjection\r\n");
        file.write("Shown\tTRUE\r\n");
        for (int index : selectedIndexes)
            file.write(String.format("Input\tRegion%d\r\n", index + 1));
        file.write("Data\r\n");
        file.write("Time (s)");
        for (int index : selectedIndexes)
            file.write(String.format("\tRegion%d ()", index + 1));
        file.write("\r\n");

        // slice numbers start with 1 for historical reasons
        double time = 0.0;
        for (int i = 1; i <= maxSlice; i += 14)
        {
            IJ.showProgress(i, maxSlice);
            file.write(String.format("%.3f", time));
            time += 1.0;
            ImageProcessor processor = stack.getProcessor(i);
            for (int index : selectedIndexes)
            {
                processor.setRoi(roiManager.getRoi(index));
                ImageStatistics stats = ImageStatistics.getStatistics(processor, options, calibration);
                file.write(String.format("\t%.3f", stats.mean));
            }
            file.write("\r\n");
        }

        file.close();
    }
}
