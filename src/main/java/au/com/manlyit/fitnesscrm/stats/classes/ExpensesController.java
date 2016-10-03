package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Expenses;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.ExpensesFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.InvoiceImages;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.extensions.event.ImageAreaSelectEvent;
import org.primefaces.model.CroppedImage;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@Named("expensesController")
@SessionScoped
public class ExpensesController implements Serializable {

    private static Logger LOGGER = Logger.getLogger(ExpensesController.class.getName());

   
    private Expenses current;
    private Expenses selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ExpensesFacade ejbFacade;
     @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceImagesFacade invoiceImagesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Expenses> filteredItems;
    private Expenses[] multiSelected;
    private boolean gstIncluded = true;
    private boolean imageAreaSelectEvent1 = false;
    private CroppedImage croppedImage;
    private BufferedImage currentImage;
    private InvoiceImages uploadedImage;
    private boolean saveButtonDisabled = true;
    private static int new_width = 800;// must match panelheight on gallery component
    private static int new_height = 500;// must match panelheight on gallery component
    private int selectionheight = 0;
    private int selectionwidth = 0;
    private int imageHeight = 0;
    private int imageWidth = 0;
    private int x1 = 0;
    private int y1 = 0;
    

    public ExpensesController() {
    }

     /**
     * @return the LOGGER
     */
    public static Logger getLOGGER() {
        return LOGGER;
    }

    /**
     * @param aLOGGER the LOGGER to set
     */
    public static void setLOGGER(Logger aLOGGER) {
        LOGGER = aLOGGER;
    }

    /**
     * @return the new_width
     */
    public static int getNew_width() {
        return new_width;
    }

    /**
     * @param aNew_width the new_width to set
     */
    public static void setNew_width(int aNew_width) {
        new_width = aNew_width;
    }

    /**
     * @return the new_height
     */
    public static int getNew_height() {
        return new_height;
    }

    /**
     * @param aNew_height the new_height to set
     */
    public static void setNew_height(int aNew_height) {
        new_height = aNew_height;
    }
    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public Expenses getSelected() {
        if (getCurrent() == null) {
            setCurrent(new Expenses());
            setSelectedItemIndex(-1);
        }
        return getCurrent();
    }

    public void setSelected(Expenses selected) {
        if (selected != null) {
            setCurrent(selected);
            setSelectedItemIndex(-1);
        }

    }

    private ExpensesFacade getFacade() {
        return getEjbFacade();
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    /**
     * @return the filteredItems
     */
    public List<Expenses> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Expenses> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Expenses[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Expenses[] multiSelected) {
        this.setMultiSelected(multiSelected);
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Expenses)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        setCurrent(new Expenses());
        setSelectedItemIndex(-1);
        return "Create";
    }

    public String create() {
        try {
            if (getCurrent().getId() == null) {
                getCurrent().setId(0);
            }
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ExpensesCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, getConfigMapFacade().getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue(ActionEvent actionEvent) {
        try {
            getCurrent().setId(0);
            getCurrent().setCreatedTimestamp(new Date());
            getCurrent().setUpdatedTimestamp(new Date());
            getCurrent().setInvoiceImageId(uploadedImage);
            //invoiceImagesFacade.create(getCurrent().getInvoiceImageId());
            getFacade().create(getCurrent());
            recreateModel();
            setCurrent(new Expenses());
            setGstIncluded(true);
            setUploadedImage(null);
            JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ExpensesCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, getConfigMapFacade().getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (Expenses)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void expenseAmountChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(BigDecimal.class)) {
            BigDecimal expenseAmount = (BigDecimal) o;
            BigDecimal multiplyPercent = new BigDecimal((getSelected().getPercentForBusinessUse()));
            BigDecimal divideGstIncluded = new BigDecimal(11);
            BigDecimal divideGstExcluded = new BigDecimal(10);
            getSelected().setBusinessUseAmount(expenseAmount.multiply(multiplyPercent));
            if (isGstIncluded()) {
                getSelected().setExpenseAmountGst(expenseAmount.divide(divideGstIncluded, 2, RoundingMode.HALF_UP));
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstIncluded, 2, RoundingMode.HALF_UP));
            } else {
                getSelected().setExpenseAmountGst(expenseAmount.divide(divideGstExcluded, 2, RoundingMode.HALF_UP));
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstExcluded, 2, RoundingMode.HALF_UP));
            }

        }
    }

    public void percentForBusinessUseChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        Float percentForBusinessUse;
        if (o.getClass().getSuperclass().equals(Number.class)) {
            percentForBusinessUse = ((Number) o).floatValue();
            if (percentForBusinessUse > 1) {
                percentForBusinessUse = (float) 1;
                getSelected().setPercentForBusinessUse((float) 1);

            }
            if (percentForBusinessUse <= 0) {
                percentForBusinessUse = (float) 0.01;
                getSelected().setPercentForBusinessUse((float) 0.01);
            }
            BigDecimal multiplyPercent = new BigDecimal((percentForBusinessUse));
            BigDecimal divideGstIncluded = new BigDecimal(11);
            BigDecimal divideGstExcluded = new BigDecimal(10);
            getSelected().setBusinessUseAmount(getSelected().getExpenseAmount().multiply(multiplyPercent));
            if (isGstIncluded()) {
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstIncluded, 2, RoundingMode.HALF_UP));
            } else {
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstExcluded, 2, RoundingMode.HALF_UP));
            }

        }
    }

    public void businessUseAmountChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(Float.class)) {
            BigDecimal businessUseAmount = (BigDecimal) o;
            // the business use amount cant be larger than the amount
            if (businessUseAmount.compareTo(getSelected().getExpenseAmount()) == 1) {
                getSelected().setBusinessUseAmount(getSelected().getExpenseAmount());
                getSelected().setPercentForBusinessUse((float) 1);
            } else {
                getSelected().setBusinessUseAmount(businessUseAmount);
                BigDecimal newPercent = businessUseAmount.divide(getSelected().getExpenseAmount(), 2, RoundingMode.HALF_UP);
                getSelected().setPercentForBusinessUse(newPercent.multiply(new BigDecimal(1)).floatValue());
            }

            BigDecimal multiplyPercent = new BigDecimal((getSelected().getPercentForBusinessUse()));

            BigDecimal divideGstIncluded = new BigDecimal(11);
            BigDecimal divideGstExcluded = new BigDecimal(10);
            getSelected().setBusinessUseAmount(getSelected().getExpenseAmount().multiply(multiplyPercent));
            if (isGstIncluded()) {
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstIncluded, 2, RoundingMode.HALF_UP));
            } else {
                getSelected().setBusinessUseAmountGst(getSelected().getBusinessUseAmount().divide(divideGstExcluded, 2, RoundingMode.HALF_UP));
            }

        }
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public String update() {
        try {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ExpensesUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, getConfigMapFacade().getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void rotateStreamedContent90degrees(ActionEvent event) {
        BufferedImage oldImage;
        //InputStream stream;

        //bais.reset();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(getUploadedImage().getImage())) {
            oldImage = ImageIO.read(bais);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Rotate image  error!! Could not read uploadedImage StreamedContent ", ex);
            return;
        }
        String fileType = "image/jpeg";
        try {
            BufferedImage img = rotateImage(90, oldImage);
            updateUploadedImage(img, fileType);
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.WARNING, "Rotate image  error!!", ex);
            JsfUtil.addErrorMessage(ex, "Rotate image  error!!");
        }
        JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ImageRotateSuccessful"));
    }

    private BufferedImage rotateImage(int degrees, BufferedImage oldImage) {
        BufferedImage newImage = null;
        if (oldImage != null) {
            newImage = new BufferedImage(oldImage.getHeight(), oldImage.getWidth(), oldImage.getType());
            Graphics2D graphics = (Graphics2D) newImage.getGraphics();
            graphics.rotate(Math.toRadians(degrees), newImage.getWidth() / 2, newImage.getHeight() / 2);
            graphics.translate((newImage.getWidth() - oldImage.getWidth()) / 2, (newImage.getHeight() - oldImage.getHeight()) / 2);
            graphics.drawImage(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), null);
        } else {
            getLOGGER().log(Level.WARNING, "rotateImage failed as the image to be rotated is NULL");
        }
        setImageAreaSelectEvent1(false);
        return newImage;

    }

    public void crop() {
        if (getUploadedImage() == null) {
            return;
        }
        if (isImageAreaSelectEvent1() == false) {
            return;
        }
        BufferedImage oldImage;
        BufferedImage img;
        String type;
        try (ByteArrayInputStream is = new ByteArrayInputStream(getUploadedImage().getImage());) {

            oldImage = ImageIO.read(is);

            // int selectionheight = imageAreaSelectEvent1.getHeight();
            // int selectionwidth = imageAreaSelectEvent1.getWidth();
            // int imageHeight = imageAreaSelectEvent1.getImgHeight();
            //  int imageWidth = imageAreaSelectEvent1.getImgWidth();
            // int x1 = imageAreaSelectEvent1.getX1();
            // int y1 = imageAreaSelectEvent1.getY1();
            //int x2 = imageAreaSelectEvent1.getX2();
            //int y2 = imageAreaSelectEvent1.getY2();
            int oldImageWidth = oldImage.getWidth();
            int oldImageHeight = oldImage.getHeight();
            float heightScaleFactor = (float) getImageHeight() / (float) oldImageHeight;
            float widthScaleFactor = (float) getImageWidth() / (float) oldImageWidth;

            Float scaledX = (float) getX1() / widthScaleFactor;
            Float scaledY = (float) getY1() / heightScaleFactor;
            Float scaledWidth = (float) getSelectionwidth() / widthScaleFactor;
            Float scaledHeight = (float) getSelectionheight() / heightScaleFactor;

            int x = scaledX.intValue();
            int y = scaledY.intValue();
            int w = scaledWidth.intValue();
            int h = scaledHeight.intValue();

            img = oldImage.getSubimage(x, y, w, h);

            switch (getUploadedImage().getImageType()) {
                case 0:
                    type = "gif";
                    break;
                case 1:
                    type = "png";
                    break;
                case 2:
                    type = "jpeg";
                    break;
                default:
                    type = "jpeg";

            }
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        updateUploadedImage(img, type);
        JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ImageCroppedSuccessful"));
        /*  String fileName = uploadedFile.getFileName();
         //String contentType = uploadedFile.getContentType();

         String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
         try {
         // InputStream is = uploadedFile.getInputstream();
         //  img = ImageIO.read(is);                                        //public BufferedImage getSubimage(int x, int y, int w, int h)
         BufferedImage cropped = currentImage.getSubimage(imageCropX1X2Y1Y2[0], imageCropX1X2Y1Y2[1], imageCropX1X2Y1Y2[2], imageCropX1X2Y1Y2[3]);
         //extension = getFileExtensionFromFilePath(uploadedFile.getContentType());
         updateImages(cropped, fileExtension);
         } catch (Exception e) {
         JsfUtil.addErrorMessage(e, "Loading image into buffer error!!");
         }
         try {

           
         //setCroppedImage(getUploadedImage());

         FileImageOutputStream imageOutput;
         try {
         imageOutput = new FileImageOutputStream(new File(uploadedImageTempFile.getAbsolutePath()));
         imageOutput.write(croppedImage.getBytes(), 0, croppedImage.getBytes().length);
         imageOutput.close();
         } catch (Exception e) {
         }
         } catch (Exception ex) {
         Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
         JsfUtil.addErrorMessage(ex, "Update image error!!");
         }*/
        setImageAreaSelectEvent1(false);
    }

    public void selectEndListener(final ImageAreaSelectEvent e) {

        setSelectionheight(e.getHeight());
        setSelectionwidth(e.getWidth());
        setImageHeight(e.getImgHeight());
        setImageWidth(e.getImgWidth());
        setX1(e.getX1());
        setY1(e.getY1());
        setImageAreaSelectEvent1(true);

    }

    private InvoiceImages processUploadedFile(UploadedFile file) {
        BufferedImage img = null;
        String fileName = file.getFileName();
        String contentType = file.getContentType();

        String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        int imgType = -1;
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            imgType = 2;
            fileExtension = "jpeg";
        }
        if (contentType.contains("png")) {
            imgType = 1;
            fileExtension = "png";
        }
        if (contentType.contains("gif")) {
            imgType = 0;
            fileExtension = "gif";
        }
        if (imgType == -1) {
            getLOGGER().log(Level.WARNING, "processUploadedFile , Cannot add invoice image as the pictureisn't a jpeg, gif or png. resource:{0}, contentType {1}", new Object[]{fileName, contentType});
            return null;
        }
        getLOGGER().log(Level.INFO, "processUploadedFile , Name of Uploaded File: {0}, contentType: {1}, file Extension:{2}", new Object[]{fileName, contentType, fileExtension});

        try (InputStream is = file.getInputstream()) {
            img = ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Loading uploaded image into buffer error!!", ex);
        }

        //BufferedImage scaledImg = resizeImageKeepAspect(img, new_width);
        BufferedImage scaledImg = resizeImageWithHintKeepAspect(img, 0, getNew_height());// use a 0 for heigh or width to keep aspect

        InvoiceImages ii = new InvoiceImages(0, new Date(), -1, imgType, convertBufferedImageToByteArray(scaledImg, fileExtension), contentType, fileName);

        //updateUploadedImage(scaledImg, fileExtension);
        //jpeg
        //BufferedImage data = null;
        //Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");
        // ImageReader reader = (ImageReader) readers.next();
        try { // get meta data from photo
            if (imgType == 2) {
                IImageMetadata metadata = Sanselan.getMetadata(file.getInputstream(), null);
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                if (jpegMetadata != null) {
                    // print out various interesting EXIF tags.
                    //for debugging only - comment out
                    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION);
                    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
                    printTagValue(jpegMetadata,
                            TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO);
                    printTagValue(jpegMetadata,
                            TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE);

                    TiffField field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    if (field == null) {

                        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.INFO, "Photo upload date not found in EXIF data");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                        String dateString = field.getValueDescription();
                        dateString = dateString.replace("'", " ").trim();
                        Date dateThePhotoWasTaken = null;
                        try {
                            ParsePosition pp = new ParsePosition(0);
                            dateThePhotoWasTaken = sdf.parse(dateString, pp);
                        } catch (Exception e) {
                            JsfUtil.addErrorMessage(e, "Couldnt get the date the photo was taken from the image file!");
                        }
                        if (dateThePhotoWasTaken != null) {
                            ii.setCreatedTimestamp(dateThePhotoWasTaken);
                        } else {
                            JsfUtil.addErrorMessage("Couldnt get the date the photo was taken from the image file!. The Date is Null!");

                        }
                        JsfUtil.addSuccessMessage("Modifying the photo taken date to the date extracted from the photo: " + dateString);

                    }

                } else {
                    JsfUtil.addErrorMessage("Couldnt get the date the photo was taken from the image file! No EXIF data in the photo.");
                }
                /* ImageInputStream iis = ImageIO.createImageInputStream(uploadedFile.getInputstream());
                 reader.setInput(iis, true);
                 IIOMetadata tags = reader.getImageMetadata(0);
                 Node tagNode = tags.getAsTree(tags.getNativeMetadataFormatName());
                 String st = tagNode.getTextContent();
                
                 data = reader.read(0);*/
            }
        } catch (IOException | ImageReadException e) {
            JsfUtil.addErrorMessage(e, "Couldnt get the date the photo was taken from the image file! No EXIF data in the photo.");
        }
        setImageAreaSelectEvent1(false);
        setUploadedImage(ii);
        return ii;
    }

    private static BufferedImage resizeImageWithHintKeepAspect(BufferedImage originalImage, int newWidth, int newHeight) {
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        float w = originalImage.getWidth();
        float h = originalImage.getHeight();
        int height = newHeight;
        int width = newWidth;

        if (newWidth == 0 && newHeight == 0) {
            return originalImage;
        }
        // if we want to keep aspect we can only use height or width - the one not used should be set to 0
        if (newWidth == 0 && newHeight > 0) {
            float aspectWidth = ((float) newHeight / h) * w;
            width = Math.round(aspectWidth);
        }
        if (newWidth > 0 && newHeight == 0) {
            float aspectHeight = ((float) newWidth / w) * h;
            height = Math.round(aspectHeight);
            width = newWidth;
        }

        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

    private static BufferedImage resizeImageKeepAspect(BufferedImage originalImage, int newWidth) {
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

        float w = originalImage.getWidth();
        float h = originalImage.getHeight();
        float newHeight = ((float) newWidth / w) * h;
        int nheight = Math.round(newHeight);

        BufferedImage resizedImage = new BufferedImage(newWidth, nheight, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, nheight, null);
        g.dispose();

        return resizedImage;
    }

    private void updateUploadedImage(BufferedImage img, String fileType) {
        getUploadedImage().setImage(convertBufferedImageToByteArray(img, fileType));
    }

    private byte[] convertBufferedImageToByteArray(BufferedImage img, String fileType) {
        byte[] ba = null;
        String type = "---";
        for (String writerName : ImageIO.getWriterFormatNames()) {
            if (fileType.toLowerCase().contains(writerName.toLowerCase())) {
                type = writerName;
                getLOGGER().log(Level.INFO, "Using IMage IO writer name : {0}", type);
            }
        }
        if (type.contains("---")) {
            type = "jpeg";
            getLOGGER().log(Level.INFO, "Using DEFAULT Image IO writer name : {0}, attempted type: {1}", new Object[]{type, fileType});
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(img, type, os);
            ba = os.toByteArray();
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Update image error!!");
        }
        return ba;
    }

    private static void printTagValue(JpegImageMetadata jpegMetadata,
            TagInfo tagInfo) {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field == null) {
            System.out.println(tagInfo.name + ": " + "Not Found.");
        } else {
            System.out.println(tagInfo.name + ": "
                    + field.getValueDescription());
        }
    }

    public void destroy() {
        performDestroy();
        recreateModel();
        setCurrent(null);
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (getSelectedItemIndex() >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    public Expenses getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Expenses selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        setCurrent(selectedForDeletion);

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(getCurrent());
            JsfUtil.addSuccessMessage(getConfigMapFacade().getConfig("ExpensesDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, getConfigMapFacade().getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (getSelectedItemIndex() >= count) {
            // selected index cannot be bigger than number of items:
            setSelectedItemIndex(count - 1);
            // go to previous page if last page disappeared:
            if (getPagination().getPageFirstItem() >= count) {
                getPagination().previousPage();
            }
        }
        if (getSelectedItemIndex() >= 0) {
            setCurrent(getFacade().findRange(new int[]{getSelectedItemIndex(), getSelectedItemIndex() + 1}).get(0));
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    private void recreateModel() {
        setItems(null);
        setFilteredItems(null);
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public void handleDateSelect(SelectEvent event) {

        Date date = (Date) event.getObject();

        //Add facesmessage
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {

        return JsfUtil.getSelectItems(getEjbFacade().findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getEjbFacade().findAll(), true);
    }

    public Collection<Expenses> getItemsAvailable() {
        return getEjbFacade().findAll();
    }

    public void onEdit(RowEditEvent event) {
        Expenses cm = (Expenses) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the gstIncluded
     */
    public boolean isGstIncluded() {
        return gstIncluded;
    }

    /**
     * @param gstIncluded the gstIncluded to set
     */
    public void setGstIncluded(boolean gstIncluded) {
        this.gstIncluded = gstIncluded;
    }

    /**
     * @return the current
     */
    public Expenses getCurrent() {
        return current;
    }

    /**
     * @param current the current to set
     */
    public void setCurrent(Expenses current) {
        this.current = current;
    }

    /**
     * @param items the items to set
     */
    public void setItems(DataModel items) {
        this.items = items;
    }

    /**
     * @return the ejbFacade
     */
    public au.com.manlyit.fitnesscrm.stats.beans.ExpensesFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     * @param ejbFacade the ejbFacade to set
     */
    public void setEjbFacade(au.com.manlyit.fitnesscrm.stats.beans.ExpensesFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    /**
     * @return the configMapFacade
     */
    public au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade getConfigMapFacade() {
        return configMapFacade;
    }

    /**
     * @param configMapFacade the configMapFacade to set
     */
    public void setConfigMapFacade(au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade) {
        this.configMapFacade = configMapFacade;
    }

    /**
     * @param pagination the pagination to set
     */
    public void setPagination(PaginationHelper pagination) {
        this.pagination = pagination;
    }

    /**
     * @return the selectedItemIndex
     */
    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    /**
     * @param selectedItemIndex the selectedItemIndex to set
     */
    public void setSelectedItemIndex(int selectedItemIndex) {
        this.selectedItemIndex = selectedItemIndex;
    }

    /**
     * @return the imageAreaSelectEvent1
     */
    public boolean isImageAreaSelectEvent1() {
        return imageAreaSelectEvent1;
    }

    /**
     * @param imageAreaSelectEvent1 the imageAreaSelectEvent1 to set
     */
    public void setImageAreaSelectEvent1(boolean imageAreaSelectEvent1) {
        this.imageAreaSelectEvent1 = imageAreaSelectEvent1;
    }

    /**
     * @return the croppedImage
     */
    public CroppedImage getCroppedImage() {
        return croppedImage;
    }

    /**
     * @param croppedImage the croppedImage to set
     */
    public void setCroppedImage(CroppedImage croppedImage) {
        this.croppedImage = croppedImage;
    }

    /**
     * @return the currentImage
     */
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * @param currentImage the currentImage to set
     */
    public void setCurrentImage(BufferedImage currentImage) {
        this.currentImage = currentImage;
    }

    /**
     * @return the uploadedImage
     */
    public InvoiceImages getUploadedImage() {
        if (uploadedImage == null) {
            uploadedImage = new InvoiceImages(0);
        }
        return uploadedImage;
    }

    public StreamedContent getUploadedImageAsStream() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
        {
            if (getUploadedImage() != null && getUploadedImage().getImage() != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(getUploadedImage().getImage()));
            } else {
                //getConfigMapFacade().getConfig("PersistenceErrorOccured")
                //InputStream iStream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/upload-invoice.png");
                return new  DefaultStreamedContent(FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/upload-invoice.png"));
            }
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
       
        current.setInvoiceImageId(processUploadedFile(event.getFile()));
        setSaveButtonDisabled(false);
        
        //RequestContext.getCurrentInstance().update("createCustomerForm");
    }

    /**
     * @param uploadedImage the uploadedImage to set
     */
    public void setUploadedImage(InvoiceImages uploadedImage) {
        this.uploadedImage = uploadedImage;
    }

    /**
     * @return the saveButtonDisabled
     */
    public boolean isSaveButtonDisabled() {
        return saveButtonDisabled;
    }

    /**
     * @param saveButtonDisabled the saveButtonDisabled to set
     */
    public void setSaveButtonDisabled(boolean saveButtonDisabled) {
        this.saveButtonDisabled = saveButtonDisabled;
    }

    /**
     * @return the selectionheight
     */
    public int getSelectionheight() {
        return selectionheight;
    }

    /**
     * @param selectionheight the selectionheight to set
     */
    public void setSelectionheight(int selectionheight) {
        this.selectionheight = selectionheight;
    }

    /**
     * @return the selectionwidth
     */
    public int getSelectionwidth() {
        return selectionwidth;
    }

    /**
     * @param selectionwidth the selectionwidth to set
     */
    public void setSelectionwidth(int selectionwidth) {
        this.selectionwidth = selectionwidth;
    }

    /**
     * @return the imageHeight
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * @param imageHeight the imageHeight to set
     */
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    /**
     * @return the imageWidth
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * @param imageWidth the imageWidth to set
     */
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    /**
     * @return the x1
     */
    public int getX1() {
        return x1;
    }

    /**
     * @param x1 the x1 to set
     */
    public void setX1(int x1) {
        this.x1 = x1;
    }

    /**
     * @return the y1
     */
    public int getY1() {
        return y1;
    }

    /**
     * @param y1 the y1 to set
     */
    public void setY1(int y1) {
        this.y1 = y1;
    }

  
    @FacesConverter(value = "expensesControllerConverter")
    public static class ExpensesControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ExpensesController controller = (ExpensesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "expensesController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Expenses) {
                Expenses o = (Expenses) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + ExpensesController.class.getName());
            }
        }

    }

}
//file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl
