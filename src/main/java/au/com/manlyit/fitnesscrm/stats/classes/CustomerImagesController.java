package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.extensions.event.ImageAreaSelectEvent;

import org.primefaces.mobile.event.SwipeEvent;
import org.primefaces.model.CroppedImage;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@Named("customerImagesController")
@SessionScoped
public class CustomerImagesController implements Serializable {

    private static final Logger logger = Logger.getLogger(CustomerImagesController.class.getName());
    private static final long serialVersionUID = 1L;
    private CustomerImages current;
    private CustomerImages lightBoxImage;
    private static final int NEW_WIDTH = 800;// must match panelheight on gallery component
    private static final int NEW_HEIGHT = 500;// must match panelheight on gallery component
    private static final int PROFILE_PIC_HEIGHT_IN_PIX = 100;
    private StreamedContent streamedCroppedImage;
    private boolean imageAreaSelectEvent1 = false;
    private CroppedImage croppedImage;
    private String newImageName;
    private String tempImageName;
    private BufferedImage currentImage;
    private int imageListSize = 0;
    private File uploadedImageTempFile;
    private CustomerImages selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomersFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private CustomerImages uploadedImage;
    private CustomerImages editingImage;
    private List<CustomerImages> images;
    private List<CustomerImages> filteredItems;
    private String effect = "fade";
    private boolean saveButtonDisabled = true;
    private UploadedFile uploadedFile;
    private boolean profilePhoto = false;
    private int numberOfMobileImagesToDisplay = 3;
    private int offsetOfMobileImagesToDisplay = 0;
    private int selectionheight = 0;
    private int selectionwidth = 0;
    private int imageHeight = 0;
    private int imageWidth = 0;
    private int x1 = 0;
    private int y1 = 0;

    public CustomerImagesController() {
    }

    @PostConstruct
    public void init() {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (loggedInUser != null) {
            Customers cust = ejbCustomersFacade.findCustomerByUsername(loggedInUser);
            createGallery(cust.getId());
            RequestContext.getCurrentInstance().update("@(.parentOfUploadPhoto) ");
        }
    }

    private int getUser() {
        return getSelectedCustomer().getId();
    }

    protected void createDefaultProfilePic(Customers cust) {
        if (cust.getCustomerImagesCollection() != null && cust.getCustomerImagesCollection().isEmpty() == true) {
            String placeholderImage = configMapFacade.getConfig("system.default.profile.image");
            String fileExtension = placeholderImage.substring(placeholderImage.lastIndexOf(".")).toLowerCase();
            int imgType = -1;
            if (fileExtension.contains("jpeg") || fileExtension.contains("jpg")) {
                imgType = 2;
                fileExtension = "jpeg";
            }
            if (fileExtension.contains("png")) {
                imgType = 1;
                fileExtension = "png";
            }
            if (fileExtension.contains("gif")) {
                imgType = 0;
                fileExtension = "gif";
            }
            if (imgType == -1) {
                logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due the picture not being in jpeg, gif or png. resource:{0}", new Object[]{placeholderImage, cust.getUsername()});
                return;
            }
            if (cust != null) {
                if (cust.getProfileImage() == null) {
                    CustomerImages ci;
                    BufferedImage img;
                    BufferedImage scaledImg = null;
                    //InputStream stream;
                    try {
                        ci = new CustomerImages(0);
                        ci.setMimeType("image/jpeg");
                        ci.setImageFileName("defaultProfilePic.jpg");
                        ci.setImageDescription("Default Avatar");
                        img = null;
                        //FacesContext context =  FacesContext.getCurrentInstance();
                        //String servPath = context.getExternalContext().getRequestServletPath() + placeholderImage;
                        //stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(servPath) ;
                        try {
                            //img = ImageIO.read(stream);
                            InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(placeholderImage);
                            //img = ImageIO.read(new URL(placeholderImage));
                            img = ImageIO.read(stream);
                              scaledImg = resizeImageWithHintKeepAspect(img, 0, NEW_HEIGHT);// use a 0 for heigh or width to keep aspect
                        } catch (IOException e) {
                            if (e.getCause().getClass() == FileNotFoundException.class) {
                                Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, File not found!!: {0}", placeholderImage);

                            } else {
                                Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, Loading image into buffer error!!", e);
                            }
                        }
                        // img = resizeImageWithHintKeepAspect(img, 0, PROFILE_PIC_HEIGHT_IN_PIX);
                        // ByteArrayOutputStream os = new ByteArrayOutputStream();
                        // try {
                        //      ImageIO.write(img, fileExtension, os);
                        //  } catch (IOException ex) {
                        //       Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, write image  error!!", ex);
                        //   }

                        ci.setImage(convertBufferedImageToByteArray(scaledImg, fileExtension));
                        ci.setImageType(imgType);
                        ci.setCustomers(cust);
                        ci.setCustomerId(cust);
                        ci.setDatetaken(new Date());

                        ejbFacade.create(ci);
                        cust.setProfileImage(ci);
                        ejbCustomersFacade.edit(cust);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due to an exception:{0}", new Object[]{e, cust.getUsername()});

                    }
                }
            } else {
                logger.log(Level.WARNING, "createDefaultProfilePic ERROR, Cannot add default profile pic to a null customer object");
            }
        }
    }

    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected();
    }

    public CustomerImages getSelected() {
        if (current == null) {
            setCurrent(getSelectedCustomer().getProfileImage());
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(CustomerImages selected) {
        this.current = selected;
    }

    public void tapHoldListener(SelectEvent event) {
        FacesMessage msg = new FacesMessage("Photo Date:",
                ((CustomerImages) event.getObject()).getFormattedDate());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void swipeRightListener(SwipeEvent event) {
        CustomerImages car = (CustomerImages) event.getData();
        //carsSmall.remove(car);
        int offset = images.size() - numberOfMobileImagesToDisplay;
        if (offsetOfMobileImagesToDisplay < offset) {
            offsetOfMobileImagesToDisplay++;
        }
        /* FacesContext.getCurrentInstance().addMessage(null,
         new FacesMessage(FacesMessage.SEVERITY_INFO,
         "IMage Swiped", "Right: " + car.getFormattedDate()));
         */
    }

    public void swipeLeftListener(SwipeEvent event) {
        CustomerImages car = (CustomerImages) event.getData();
        if (offsetOfMobileImagesToDisplay > 0) {
            offsetOfMobileImagesToDisplay--;
        }
        //carsSmall.remove(car);

        /*FacesContext.getCurrentInstance().addMessage(null,
         new FacesMessage(FacesMessage.SEVERITY_INFO,
         "Image  Swiped", "Left: " + car.getFormattedDate()));
         */
    }

    private void createGallery(int customerId) {
        try {
            List<CustomerImages> imageList = getFacade().findAllByCustId(customerId, true);

            images = new ArrayList<>();

            for (CustomerImages ci : imageList) {
                if (ci != null && ci.getImage() != null) {
                    images.add(ci);
                }
            }
            imageListSize = images.size();
            logger.log(Level.INFO, "createGallery for customer ID:{2}. Images retrieved from DB : {0}. ImageList Size: {1}", new Object[]{imageList.size(), imageListSize, customerId});
        } catch (Exception e) {
            logger.log(Level.WARNING, "createGallery for" + customerId + " error :", e);
        }

    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile file) {
        this.uploadedFile = file;
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

    public void handleCustomerChange(ValueChangeEvent event) {
        String message = "Error setting customer name for photo upload";
        Object o = event.getNewValue();
        if (o != null) {
            if (o.getClass() == Customers.class) {
                Customers cust = (Customers) o;
                message = "Customer's name for photo upload is: " + cust.getUsername();
                JsfUtil.addSuccessMessage(message);
            } else {
                JsfUtil.addErrorMessage(message);
            }
        } else {
            JsfUtil.addErrorMessage(message);
        }

    }

    private void processUploadedFile(UploadedFile file) {
        BufferedImage img = null;
        String fileName = file.getFileName();
        String contentType = file.getContentType();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

        int imgType = -1;
        if (contentType.contains("jpeg")) {
            imgType = 2;
            fileExtension = "jpeg";
        }
        if (contentType.contains("jpg")) {
            imgType = 2;
            fileExtension = "jpg";
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
            logger.log(Level.WARNING, "processUploadedFile , Cannot add default profile pic  due the picture not being in jpeg, gif or png. resource:{0}, contentType {1}", new Object[]{fileName, contentType});
            return;
        }
        logger.log(Level.INFO, "processUploadedFile , Name of Uploaded File: {0}, contentType: {1}, file Extension:{2}", new Object[]{fileName, contentType, fileExtension});
        CustomerImages ci = getUploadedImage();
        ci.setImageFileName(fileName);
        ci.setMimeType(contentType);
        ci.setImageType(imgType);
        if (imgType == 2) {
            ci.setDatetaken(getDateFromJpegMetadata(file));
        }
        setUploadedImage(ci);

        try (InputStream is = file.getInputstream()) {
            img = ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Loading uploaded image into buffer error!!", ex);
        }

        //BufferedImage scaledImg = resizeImageKeepAspect(img, NEW_WIDTH);
        BufferedImage scaledImg = resizeImageWithHintKeepAspect(img, 0, NEW_HEIGHT);// use a 0 for heigh or width to keep aspect
        updateUploadedImage(scaledImg, fileExtension);

        //setCroppedImage(scaledImg, fileExtension);
        //jpeg

        //BufferedImage data = null;
        //Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");
        // ImageReader reader = (ImageReader) readers.next();
        imageAreaSelectEvent1 = false;
    }

   /* private void setCroppedImage(BufferedImage file, String fileType) {
        if (file == null) {
            return;
        }
        try {
            byte[] ba = convertBufferedImageToByteArray(file, fileType);
            Path tmpfile;
            tmpfile = Files.createTempFile( getRandomImageName() + "-", "."+ fileType);
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.INFO, "setCroppedImage - tempfile name : {0}", tmpfile.getFileName().toString());
            try (InputStream input = new ByteArrayInputStream(ba)) {
                Files.copy(input, tmpfile, StandardCopyOption.REPLACE_EXISTING);
            }


            
           

            setTempImageName(tmpfile.toString());
        } catch (IOException e) {

            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.WARNING, "setCroppedImage failed", e);
            return;
        }
        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.INFO, "setCroppedImage finished");

    }*/

    private Date getDateFromJpegMetadata(UploadedFile file) {
        Date photoDateFromMeta = null;
        try { // get meta data from photo
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
                        photoDateFromMeta = dateThePhotoWasTaken;

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
        } catch (IOException | ImageReadException e) {
            JsfUtil.addErrorMessage(e, "Couldnt get the date the photo was taken from the image file! No EXIF data in the photo.");
        }
        return photoDateFromMeta;
    }

    public void selectEndListener(final ImageAreaSelectEvent e) {

        selectionheight = e.getHeight();
        selectionwidth = e.getWidth();
        imageHeight = e.getImgHeight();
        imageWidth = e.getImgWidth();
        x1 = e.getX1();
        y1 = e.getY1();
        imageAreaSelectEvent1 = true;
        logger.log(Level.INFO, "selectEndListener  ImageAreaSelectEvent", e);

    }

    private BufferedImage convertByteArrayToBufferedImage(byte[] ba) {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);

        try {
            return ImageIO.read(bais);
        } catch (IOException e) {
            logger.log(Level.WARNING, "convertByteArrayToBufferedImage FAILED", e);
            return null;
        }
    }

    private byte[] convertBufferedImageToByteArray(BufferedImage img, String fileType) {
        byte[] ba = null;
        String type = "---";
        try {
            for (String writerName : ImageIO.getWriterFormatNames()) {
                if (fileType.toLowerCase().contains(writerName.toLowerCase())) {
                    type = writerName;
                    //logger.log(Level.INFO, "Using IMage IO writer name : {0}", type);
                }
            }
            if (type.contains("---")) {
                type = "jpeg";
                logger.log(Level.INFO, "Using DEFAULT Image IO writer name : {0}, attempted type: {1}", new Object[]{type, fileType});
            }
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                ImageIO.write(img, type, os);
                ba = os.toByteArray();
            } catch (Exception ex) {
                Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, "Update image error!!");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "convertBufferedImageToByteArray method failed", e);
        }
        return ba;
    }

    private void updateUploadedImage(BufferedImage img, String fileType) {
        uploadedImage.setImage(convertBufferedImageToByteArray(img, fileType));
    }

    /*  private void updateImages(BufferedImage img, String fileType) {

     ByteArrayOutputStream os = new ByteArrayOutputStream();
     try {

     ImageIO.write(img, fileType, os);

     } catch (IOException ex) {
     Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
     JsfUtil.addErrorMessage(ex, "Scaling image  error!!");
     }
     //getSelected().setImage(os.toByteArray());
     //InputStream stream = new ByteArrayInputStream(os.toByteArray());
     try {
     ImageIO.write(img, fileType, os);
     uploadedImage.setImage(os.toByteArray());
     // setUploadedImage(new DefaultStreamedContent(stream, "image/jpeg"));
     // currentImage = img;
     //setCroppedImage(getUploadedImage());
     } catch (Exception ex) {
     Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
     JsfUtil.addErrorMessage(ex, "Update image error!!");
     }

     }*/
    public void rotateEditedImage90degrees(ActionEvent event) {
        editingImage = rotateImage(editingImage);
    }

    public void rotateStreamedContent90degrees(ActionEvent event) {
        uploadedImage = rotateImage(uploadedImage);
    }

    private CustomerImages rotateImage(CustomerImages ci) {

        BufferedImage oldImage;
        //InputStream stream;

        //bais.reset();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(ci.getImage())) {
            oldImage = ImageIO.read(bais);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Rotate image  error!! Could not read uploadedImage StreamedContent ", ex);
            return null;
        }
        String fileType = "image/jpeg";
        try {
            BufferedImage img = rotateImage(90, oldImage);
            ci.setImage(convertBufferedImageToByteArray(img, fileType));
            // updateUploadedImage(img, fileType);
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.WARNING, "Rotate image  error!!", ex);
            JsfUtil.addErrorMessage(ex, "Rotate image  error!!");
        }
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ImageRotateSuccessful"));
        return ci;
    }

    public void rotate90degrees(ActionEvent event) {
        BufferedImage oldImage;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(current.getImage());
            oldImage = ImageIO.read(is);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        BufferedImage img = rotateImage(90, oldImage);
        int newWidth = 0;
        int newHeight = NEW_HEIGHT;

        BufferedImage scaledImg = resizeImageWithHintKeepAspect(img, newWidth, newHeight);
        String type;
        switch (current.getImageType()) {
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
        updateUploadedImage(scaledImg, type);
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("IMageRotateSuccessful"));
        imageAreaSelectEvent1 = false;
    }

    public void cropEditedImage(ActionEvent event) {
        editingImage = cropImage(editingImage, imageAreaSelectEvent1);
    }

    public void crop() {
         if (imageAreaSelectEvent1 == true) {
            uploadedImage = cropImage(uploadedImage, imageAreaSelectEvent1);
        }else{
             JsfUtil.addErrorMessage("Error",configMapFacade.getConfig("NoCroppedAreaSelected"));
         }
        

    }

  /*  public void crop() {
        if (croppedImage == null) {
            return;
        }

        setNewImageName(getRandomImageName());
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String newFileName = externalContext.getRealPath("") + File.separator + "resources"
                + File.separator + "images" + File.separator + "crop" + File.separator + getNewImageName() + ".jpg";

        FileImageOutputStream imageOutput;
        try {
            imageOutput = new FileImageOutputStream(new File(newFileName));
            imageOutput.write(croppedImage.getBytes(), 0, croppedImage.getBytes().length);
            imageOutput.close();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cropping failed."));
            return;
        }

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Cropping finished."));
    }*/

    private String getRandomImageName() {
        int i = (int) (Math.random() * 100000);

        return String.valueOf(i);
    }

    private CustomerImages cropImage(CustomerImages ci, boolean imageAreaSelectEvent1) {
        if (ci == null) {
            return null;
        }
        if (imageAreaSelectEvent1 == false) {
            return null;
        }
        BufferedImage oldImage;
        BufferedImage img;
        String type;
        try (ByteArrayInputStream is = new ByteArrayInputStream(ci.getImage());) {

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
            float heightScaleFactor = (float) imageHeight / (float) oldImageHeight;
            float widthScaleFactor = (float) imageWidth / (float) oldImageWidth;

            Float scaledX = (float) x1 / widthScaleFactor;
            Float scaledY = (float) y1 / heightScaleFactor;
            Float scaledWidth = (float) selectionwidth / widthScaleFactor;
            Float scaledHeight = (float) selectionheight / heightScaleFactor;

            int x = scaledX.intValue();
            int y = scaledY.intValue();
            int w = scaledWidth.intValue();
            int h = scaledHeight.intValue();

            img = oldImage.getSubimage(x, y, w, h);

            switch (uploadedImage.getImageType()) {
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
            return null;
        }
        // updateUploadedImage(img, type);
        ci.setImage(convertBufferedImageToByteArray(img, type));
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ImageCroppedSuccessful"));
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
        imageAreaSelectEvent1 = false;
        return ci;
    }

    public void handleFileUpload(FileUploadEvent event) {
//Barefoot-image_100_by_100.jpg

        setUploadedFile(event.getFile());
        if (getUploadedFile() != null) {
            processUploadedFile(getUploadedFile());
        } else {
            JsfUtil.addErrorMessage("The file upload failed. Please try again.");
        }
        setSaveButtonDisabled(false);
        //RequestContext.getCurrentInstance().update("createCustomerForm");
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
            logger.log(Level.WARNING, "rotateImage failed as the image to be rotated is NULL");
        }
        imageAreaSelectEvent1 = false;
        return newImage;

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

    private CustomerImagesFacade getFacade() {
        return ejbFacade;
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel<CustomerImages> createPageDataModel() {
                    return new ListDataModel<>(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    private BufferedImage rotateBufferedImage(BufferedImage oldImage, int degreesToRotate) {
        return rotateImage(degreesToRotate, oldImage);

    }

    public void prepareViewFromStatTakenController() {
        try {
            setCurrent((CustomerImages) getItems().getRowData());
            loadImage(current);
            selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
        }
    }

    public void prepareCreateFromStatTakenController(CustomerImages ci) {
        try {
            setCurrent(ci);
            loadImage(current);
            selectedItemIndex = -1;
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
        }
    }

    public void prepareEditFromStatTakenController() {
        try {
            setCurrent((CustomerImages) getItems().getRowData());
            loadImage(current);
            selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
        }
    }

    public String prepareView() {
        //setCurrent((CustomerImages) getItems().getRowData());
        loadImage(current);
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public void prepareCreateMobile() {
        clearPhoto();
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            ec.redirect(request.getContextPath() + "/trainer/sessionHistory/CreatePhotoMobile.xhtml");
        } catch (IOException ex) {
            Logger.getLogger(SessionHistoryController.class.getName()).log(Level.SEVERE, "prepareCreateMobileReturnCreate", ex);
        }
    }

    public String prepareCreate() {
        clearPhoto();
        return "Create";
    }

    private void clearPhoto() {
        setCurrent(null);
        loadImage(current);
        selectedItemIndex = -1;
        setSaveButtonDisabled(true);
        setProfilePhoto(false);
    }

    public String prepareEdit() {
        // setCurrent((CustomerImages) getItems().getRowData());
        loadImage(current);
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void createFromMobile() {
        if (getUploadedFile() != null) {
            processUploadedFile(getUploadedFile());
            createFromUploadDialogue();
            logger.log(Level.INFO, "Photo upload successfully from mobile device.");
        } else {
            logger.log(Level.WARNING, "The uploaded photo is NULL. Photo upload from mobile device failed.");
        }

    }

    public void createFromUploadDialogue() {
        current = uploadedImage;
        createFromDialogue();
        uploadedImage = null;
        RequestContext.getCurrentInstance().update("@(.parentOfUploadPhoto) ");
    }

    public void createFromDialogue() {

        createFromListener();
        clearPhoto();
        recreateModel();
    }

    private void createFromListener() {
        try {
            current.setId(0);// auto generated by DB , but cannot be null 
            Customers cust = getSelectedCustomer();
            if (current.getCustomerId() == null) {
                current.setCustomerId(cust);
            }
            if (current.getDatetaken() == null) {
                current.setDatetaken(new Date());
            }
            if (current.getImageFileName() == null) {
                current.setImageFileName("");
            }
            if (current.getMimeType() == null) {
                current.setMimeType("image/jpeg");
            }
            getFacade().create(current);
            if (isProfilePhoto()) {
                // ejbCustomersFacade.setCustomerProfileImage(getSelectedCustomer().getId(), current);
                cust.setProfileImage(current);
                ejbCustomersFacade.edit(cust);
                /* FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                custController.setSelected(cust);*/
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesCreated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public String create() {
        createFromListener();

        return prepareCreate();
    }

    public String create(Date date) {
        try {
            current.setId(0); // auto generated by DB , but cannot be null    \n
            current.setDatetaken(date);
            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    private String getImageTypeString(int imtyp) {
        String type = "image/jpeg";

        switch (imtyp) {
            case 1:
                return "image/png";
            case 2:
                return "image/jpeg";
        }

        return type;

    }

    private String getFileExtensionFromFilePath(String path) {
        return path.substring(path.lastIndexOf(".")).toLowerCase();

    }

    private void loadImage(CustomerImages ci) {
        String placeholderImage = "/resources/images/Barefoot-image_100_by_100.jpg";
        uploadedImage = loadDefaultImageIfNull(ci, placeholderImage);

    }

    private CustomerImages loadDefaultImage(CustomerImages ci) {
        String placeholderImage = "/resources/images/Barefoot-image_100_by_100.jpg";
        return loadDefaultImageIfNull(ci, placeholderImage);

    }

    private CustomerImages loadDefaultImageIfNull(CustomerImages ci, String defaultImagePath) {
        CustomerImages sc = null;
        BufferedImage img = null;
        if (ci == null || ci.getImage() == null) {
            try {
                // get default image
                InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(defaultImagePath);
                if (stream != null) {
                    sc = new CustomerImages(0);
                    sc.setDatetaken(new Date());
                    sc.setImageDescription("");
                    Path p = Paths.get(defaultImagePath);
                    //ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        String fileName = p.getFileName().toString();
                        sc.setImageFileName(fileName);
                        String fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                        //String imageType = "image/" + fileExtension;
                        img = ImageIO.read(stream);
                        //ImageIO.write(img, imageType, os);
                        sc.setImage(convertBufferedImageToByteArray(img, fileExtension));

                    } catch (Exception ex) {
                        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Loading uploaded image into buffer error!!", ex);
                    }
                } else {
                    Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Couldn't load the default picture");
                }
            } catch (Exception e) {
                String message = "Could not load default image:" + defaultImagePath;
                JsfUtil.addErrorMessage(message);
                logger.log(Level.WARNING, message, e);
            }
            imageAreaSelectEvent1 = false;
        } else {
            return ci;
            //ByteArrayInputStream is = new ByteArrayInputStream(ci.getImage());
            //String imgtyp = getImageTypeString(ci.getImageType());
            //sc = new DefaultStreamedContent(is, imgtyp);
        }
        return sc;
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void updateEditedPhoto() {
        try {
            getFacade().edit(getEditingImage());
            if (isProfilePhoto()) {
                //ejbCustomersFacade.setCustomerProfileImage(getSelectedCustomer().getId(), current);
                Customers cust = getSelectedCustomer();
                cust.setProfileImage(current);
                ejbCustomersFacade.edit(cust);
                /*FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                custController.setSelected(cust);*/
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesUpdated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public String destroy() {
        setCurrent((CustomerImages) getItems().getRowData());
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public void destroyListener() {
        performDestroy();
        recreateModel();
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            setCurrent(getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0));
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    public void recreateModel() {
        items = null;
        filteredItems = null;
        images = null;
        imageAreaSelectEvent1 = false;

    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    /*
    
    
     The <p:graphicImage> requires a special getter method. It will namely be invoked twice per generated image, each in a completely different HTTP request.

     The first HTTP request, which has requested the HTML result of a JSF page, will invoke the getter for the first time in order to generate the HTML <img> element with the right unique and auto-generated URL in the src attribute which contains information about which bean and getter exactly should be invoked whenever the webbrowser is about to request the image. Note that the getter does at this moment not need to return the image's contents. It would not be used in any way as that's not how HTML works (images are not "inlined" in HTML output, but they are instead requested separately).

     Once the webbrowser retrieves the HTML result as HTTP response, it will parse the HTML source in order to present the result visually to the enduser. Once the webbrowser encounters an <img> element during parsing the HTML source, then it will send a brand new HTTP request on the URL as specified in its src attribute in order to download the content of that image and embed it in the visual presentation. This will invoke the getter method for the second time which in turn should return the actual image content.

     In your particular case PrimeFaces was apparently either unable to identify and invoke the getter in order to retrieve the actual image content, or the getter didn't return the expected image content. The usage of #{item} variable name and the lot of calls in the log suggests that you were using it in an <ui:repeat> or a <h:dataTable>. Most likely the backing bean is request scoped and the datamodel isn't properly preserved during the request for the image and JSF won't be able to invoke the getter during the right iteration round. A view scoped bean would also not work as the JSF view state is nowhere available when the browser actually requests the image.

     To solve this problem, your best bet is to rewrite the getter method as such so that it can be invoked on a per-request basis wherein you pass the unique image identifier as a <f:param> instead of relying on some backing bean properties which may go "out of sync" during subsequent HTTP requests. It would make completely sense to use a separate application scoped managed bean for this which doesn't have any state. Moreover, an InputStream can be read only once, not multiple times.

     In other words: never declare StreamedContent nor any InputStream as a bean property; only create it brand-new in the getter when the webbrowser actually requests the image content.

     E.g.

     <p:dataTable value="#{customerImagesController.images}" var="item">
     <p:column>
     <p:graphicImage value="#{customerImagesController.image}">
     <f:param name="imageId" value="#{item.id}" />
     </p:graphicImage>
     </p:column>
     </p:dataTable>
    
    
     */
    public void carouselImageClicked(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        String imageId = context.getExternalContext().getRequestParameterMap().get("imageId");
        if (imageId != null) {
            setUploadedImage(ejbFacade.find(Integer.valueOf(imageId)));
        }
        RequestContext.getCurrentInstance().openDialog("uploadPhotoDialogueWidget");
    }

    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        String imageId;
        if (context != null) {
            //if (context.getRenderResponse()) {
            if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
                // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
                logger.log(Level.INFO, "getImage: we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.");
                return new DefaultStreamedContent();
            } else // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            {
                imageId = context.getExternalContext().getRequestParameterMap().get("imageId");
                if (imageId != null) {
                    CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                    if (custImage.getCustomerId().getId().compareTo(getSelectedCustomer().getId()) == 0) {
                        logger.log(Level.INFO, "getImage - returning image:{0},size:{1}, name:{2}, Encoding:{3}, Content Type: {4} ", new Object[]{imageId, custImage.getImage().length, custImage.getImageFileName(), custImage.getImageStream().getContentEncoding(), custImage.getImageStream().getContentType()});
                        return new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));
                    } else {
                        logger.log(Level.WARNING, "A customer is attempting to access anothers images by directly manipulating the URL posted parameters. It might be a hacker. Returning NULL instead of the image. Logged In Customer:{0},Imageid:{1}", new Object[]{getSelectedCustomer().getUsername(), imageId});
                        return null;
                    }
                } else {
                    logger.log(Level.WARNING, "getImage: imageId is NULL");
                    return null;
                }
            }
        }
        return null;
    }

    public StreamedContent getImageThumbnail() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        int thumbnailWidth = 50;
        String imageId;
        if (context != null) {
            //if (context.getRenderResponse()) {
            if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
                // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
                logger.log(Level.FINE, "getImageThumbnail: we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.");
                return new DefaultStreamedContent();
            } else // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            {

                imageId = context.getExternalContext().getRequestParameterMap().get("imageId");

                if (imageId != null && !imageId.trim().isEmpty()) {
                    try {
                        CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                        String imageType = custImage.getMimeType().toLowerCase();
                        if (imageType.contains("jpeg") || imageType.contains("jpg")) {
                            imageType = "jpeg";
                        } else if (imageType.contains("gif")) {
                            imageType = "gif";
                        } else if (imageType.contains("png")) {
                            imageType = "png";
                        } else {
                            imageType = "jpeg";
                            logger.log(Level.WARNING, "getImageThumbnail: The Image Type could not be determined. Trying JPEG. ImageId=", imageId);
                        }
                        return new DefaultStreamedContent(new ByteArrayInputStream(convertBufferedImageToByteArray(resizeImageKeepAspect(convertByteArrayToBufferedImage(custImage.getImage()), thumbnailWidth), imageType)));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "getImageThumbnail: the imageId parameter could not be converted to an integer:{0}, exception: {1} ", new Object[]{imageId, e.getMessage()});
                    }
                } else {
                    logger.log(Level.WARNING, "getImageThumbnail: imageId is NULL or an empty String id:{0}", new Object[]{imageId});
                    return null;
                }
            }
        }
        return null;
    }


    /*
    //old version of get image
     public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            String imageId = context.getExternalContext().getRequestParameterMap().get("imageId");
            if (imageId != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                if (custImage.getCustomerId().getId().compareTo(getSelectedCustomer().getId()) == 0) {
                    return new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));
                } else {
                    logger.log(Level.WARNING, "A customer is attempting to access anothers images by directly manipulating the URL posted parameters. It might be a hacker. Returning NULL instead of the image. Logged In Customer:{0},Imageid:{1}", new Object[]{getSelectedCustomer().getUsername(), imageId});
                    return null;
                }
            } else {
                return null;
            }
        }
    }
     */
    public void editPictureListener() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String[]> paramValues = context.getExternalContext().getRequestParameterValuesMap();
        String[] selectedImages = paramValues.get("editImage");

        for (String item : selectedImages) {
            if (item != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(item));
                if (custImage != null) {
                    setEditingImage(custImage);
                    RequestContext.getCurrentInstance().execute("PF('editPhotoDialogueWidget').show();");
                }
            }
        }
        imageAreaSelectEvent1 = false;

    }

    public void uploadPictureListener() {

        setUploadedImage(null);

    }

    public void removePicture() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String[]> paramValues = context.getExternalContext().getRequestParameterValuesMap();
        String[] selectedImages = paramValues.get("selectedImage");

        for (String item : selectedImages) {
            if (item != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(item));
                if (custImage != null) {
                    CustomerImagesController controller = (CustomerImagesController) context.getApplication().getELResolver().
                            getValue(context.getELContext(), null, "customerImagesController");
                    try {
                        if (Objects.equals(custImage.getCustomerId().getProfileImage().getId(), custImage.getId())) {
                            JsfUtil.addSuccessMessage("Photo Not Removed", "Your profile photo cannot be deleted. Upload a new picture to enable deletion of this one.");

                        } else if (custImage.getCustomerId().getCustomerImagesCollection().size() == 1) {
                            JsfUtil.addSuccessMessage("Photo Not Removed", "The last photo cannot be removed");

                        } else {

                            ejbFacade.remove(custImage);
                            controller.removeImageFromList(custImage);
                            RequestContext.getCurrentInstance().update("@(.parentOfUploadPhoto) ");

                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed To Remove Photo!", e);

                    }
                }
            }
        }
        imageAreaSelectEvent1 = false;
    }

    public StreamedContent getUploadedImageAsStream() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            return new DefaultStreamedContent(new ByteArrayInputStream(getUploadedImage().getImage()));
        }
    }

    public StreamedContent getEditedImageAsStream() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            return new DefaultStreamedContent(new ByteArrayInputStream(getEditingImage().getImage()));
        }
    }

    public StreamedContent getLightBoxImageAsStream() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            return new DefaultStreamedContent(new ByteArrayInputStream(getLightBoxImage().getImage()));
        }
    }

    /**
     * @return the uploadedImage
     */
    public CustomerImages getUploadedImage() {
        if (uploadedImage == null) {
            try {
                // get default image 
                // if (current == null) {
                //    setCurrent(getSelectedCustomer().getProfileImage());
                // }
                loadImage(null);
                //InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                //uploadedImage = new DefaultStreamedContent(stream, "image/jpeg");
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
            }
            //  RequestContext.getCurrentInstance().update("@(.dialoguePhoto)");
        }
        return uploadedImage;
    }

    public String getDynamicTitle() {
        String title = "";
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imageId");
        if (id != null) {
            Integer imageId = Integer.parseInt(id);
            CustomerImages ci = ejbFacade.find(imageId);
            title = ci.getCustomerId().getFirstname() + " " + ci.getCustomerId().getLastname();
        }
        return title;
    }

    public String getDynamicDescription() {
        String desc = "";
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imageId");
        if (id != null) {
            Integer imageId = Integer.parseInt(id);
            CustomerImages ci = ejbFacade.find(imageId);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            desc = sdf.format(ci.getDatetaken());
        }
        return desc;
    }

    public StreamedContent getDynamicImage() {
        StreamedContent sc = null;
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imageId");

        try {
            if (id != null && id.trim().isEmpty() == false) {
                Integer imageId = Integer.parseInt(id);
                CustomerImages ci = ejbFacade.find(imageId);
                current = ci;
                RequestContext.getCurrentInstance().update(":myStatsForm:photo2");
                sc = ci.getImageStream();
            } else {
                try {
                    // get default image
                    //if you return null here then it won't work!!! You have to return something.
                    InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                    sc = new DefaultStreamedContent(stream, "image/jpeg");
                } catch (Exception e) {
                    JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
                }
            }
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "getDynamicImage number format exception for {0}", id);
        }

        return sc;
    }

    public void handleDateSelect(SelectEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
        Date date = (Date) event.getObject();
        current.setDatetaken(date);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(date)));
    }

    /**
     * @param uploadedImage the uploadedImage to set public class PictureBean {
     * private StreamedContent myImage; public PictureBean() { InputStream
     * inputStream = //InputStream of a blob myImage = new
     * DefaultStreamedContent(inputStream, "image/png"); } public
     * StreamedContent getMyImage(){ return myImage; } public void
     * setMyImage(StreamedContent myImage){ this.myImage = myImage; } }
     *
     */
    public void setUploadedImage(CustomerImages uploadedImage) {
        this.uploadedImage = uploadedImage;

    }

    public void removeImageFromList(CustomerImages image) {

        images.remove(image);
        imageListSize = images.size();
        RequestContext.getCurrentInstance().update("@(.parentOfUploadPhoto) ");

    }

    public List<CustomerImages> getImages() {
        if (images == null) {
            createGallery(getUser());
            if (images == null) {
                images = new ArrayList<>();
            }

        }
        RequestContext.getCurrentInstance().update("@(.parentOfUploadPhoto) ");
        return images;
    }

    public List<CustomerImages> getMobileImages() {
        ArrayList<CustomerImages> mobImages = null;
        int size = 0;
        try {
            if (images == null) {
                createGallery(getUser());
                if (images == null) {
                    images = new ArrayList<>();
                }
            }
            size = images.size();
            mobImages = new ArrayList<>();
            int count = 0;
            for (int c = offsetOfMobileImagesToDisplay; c < size; c++) {
                if (count < numberOfMobileImagesToDisplay) {
                    mobImages.add(images.get(c));
                }
                count++;

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "getMobileImages", e);
        }

        logger.log(Level.INFO, "getMobileImages - offset={0},Display={1},backing array Size ={2}, user:{3}, images:{4}", new Object[]{offsetOfMobileImagesToDisplay, numberOfMobileImagesToDisplay, size, getUser(), mobImages.size()});

        return mobImages;
    }

    /**
     * @param images the images to set
     */
    public void setImages(List<CustomerImages> images) {
        this.images = images;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    /**
     * @param current the current to set
     */
    public void setCurrent(CustomerImages current) {
        this.current = current;
        this.loadImage(current);
    }

    /**
     * @return the saveButtonEnabled
     */
    public boolean isSaveButtonDisabled() {
        return saveButtonDisabled;
    }

    /**
     * @param saveButtonEnabled the saveButtonEnabled to set
     */
    public void setSaveButtonDisabled(boolean saveButtonEnabled) {
        this.saveButtonDisabled = saveButtonEnabled;
    }

    /**
     * @return the selectedForDeletion
     */
    public CustomerImages getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(CustomerImages selectedForDeletion) {
        if (selectedForDeletion != null) {
            this.selectedForDeletion = selectedForDeletion;
            current = selectedForDeletion;

            performDestroy();
            recreateModel();
        }

    }

    /**
     * @return the filteredItems
     */
    public List<CustomerImages> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<CustomerImages> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the croppedImage
     */
    public StreamedContent getCroppedImage() {
        return streamedCroppedImage;
    }

    /**
     * @param croppedImage the croppedImage to set
     */
    public void setCroppedImage(StreamedContent croppedImage) {
        this.streamedCroppedImage = croppedImage;
    }

    /**
     * @return the cropperImage
     */
    public CroppedImage getCropperImage() {
        return croppedImage;
    }

    /**
     * @param cropperImage the cropperImage to set
     */
    public void setCropperImage(CroppedImage cropperImage) {
        this.croppedImage = cropperImage;
    }

    /**
     * @return the profilePhoto
     */
    public boolean isProfilePhoto() {
        return profilePhoto;
    }

    /**
     * @param profilePhoto the profilePhoto to set
     */
    public void setProfilePhoto(boolean profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    /**
     * @return the uploadedImageTempFile
     */
    public File getUploadedImageTempFile() {
        return uploadedImageTempFile;
    }

    /**
     * @param uploadedImageTempFile the uploadedImageTempFile to set
     */
    public void setUploadedImageTempFile(File uploadedImageTempFile) {
        this.uploadedImageTempFile = uploadedImageTempFile;
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
     * @return the lightBoxImage
     */
    public CustomerImages getLightBoxImage() {
        return lightBoxImage;
    }

    /**
     * @param lightBoxImage the lightBoxImage to set
     */
    public void setLightBoxImage(CustomerImages lightBoxImage) {
        this.lightBoxImage = lightBoxImage;
    }

    /**
     * @return the imageListSize
     */
    public int getImageListSize() {
        return getImages().size();
    }

    /**
     * @param imageListSize the imageListSize to set
     */
    public void setImageListSize(int imageListSize) {
        this.imageListSize = imageListSize;
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

    /**
     * @return the editingImage
     */
    public CustomerImages getEditingImage() {
        if (editingImage == null) {
            try {
                editingImage = loadDefaultImage(null);
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
            }
        }
        return editingImage;
    }

    /**
     * @param editingImage the editingImage to set
     */
    public void setEditingImage(CustomerImages editingImage) {
        this.editingImage = editingImage;
    }

    @FacesConverter(value = "customerImagesControllerConverter")
    public static class CustomerImagesControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CustomerImagesController controller = (CustomerImagesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "customerImagesController");
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuffer sb = new StringBuffer();
            sb.append(value);
            return sb.toString();
        }

        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof CustomerImages) {
                CustomerImages o = (CustomerImages) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + CustomerImagesController.class.getName());
            }
        }
    }

    /**
     * @return the newImageName
     */
    public String getNewImageName() {
        return newImageName;
    }

    /**
     * @param newImageName the newImageName to set
     */
    public void setNewImageName(String newImageName) {
        this.newImageName = newImageName;
    }

    /**
     * @return the tempImageName
     */
    public String getTempImageName() {
        return tempImageName;
    }

    /**
     * @param tempImageName the tempImageName to set
     */
    public void setTempImageName(String tempImageName) {
        this.tempImageName = tempImageName;
    }
}
/*
 *   This goes in CustomerImages - it gets wiped if its regenerated from DB
 *
 *    public StreamedContent getImageStream() {
 StreamedContent sc = null;
 if (getImage() == null) {
 try {
 // get default image
 InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
 sc = new DefaultStreamedContent(stream, "image/jpeg");
 } catch (Exception e) {
 JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
 }
 } else {
 String type = "image/jpeg";
 switch (getImageType()) {
 case 1:
 type = "image/png";
 break;
 case 2:
 type = "image/jpeg";
 break;
 }
 ByteArrayInputStream is = new ByteArrayInputStream(getImage());
 sc = new DefaultStreamedContent(is, type);
 }

 return sc;
 }
 */
