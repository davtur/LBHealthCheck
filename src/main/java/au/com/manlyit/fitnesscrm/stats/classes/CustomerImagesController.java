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
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.primefaces.model.CroppedImage;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@Named("customerImagesController")
@SessionScoped
public class CustomerImagesController implements Serializable {

    private static final Logger logger = Logger.getLogger(CustomerImagesController.class.getName());
    private CustomerImages current;
    private static final int new_width = 800;// must match panelheight on gallery component
    private static final int new_height = 500;// must match panelheight on gallery component
    private static final int PROFILE_PIC_HEIGHT_IN_PIX = 100;
    private StreamedContent streamedCroppedImage;
    private CroppedImage croppedImage;
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
    private StreamedContent uploadedImage;
    private List<CustomerImages> images;
    private List<CustomerImages> filteredItems;
    private String effect = "fade";
    private boolean saveButtonDisabled = true;
    private UploadedFile uploadedFile;
    private boolean profilePhoto = false;

    public CustomerImagesController() {
    }

    @PostConstruct
    public void init() {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Customers cust = ejbCustomersFacade.findCustomerByUsername(loggedInUser);
        createGallery(cust.getId());
    }

    private int getUser() {
        return getSelectedCustomer().getId();
    }

    protected void createDefaultProfilePic(Customers cust) {
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
                //InputStream stream;
                try {
                    ci = new CustomerImages(0);
                    img = null;
                    //FacesContext context =  FacesContext.getCurrentInstance();
                    //String servPath = context.getExternalContext().getRequestServletPath() + placeholderImage;
                    //stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(servPath) ;
                    try {
                        //img = ImageIO.read(stream);
                        img = ImageIO.read(new URL(placeholderImage));
                    } catch (IOException e) {
                        if (e.getCause().getClass() == FileNotFoundException.class) {
                            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, File not found!!: {0}", placeholderImage);

                        } else {
                            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, Loading image into buffer error!!", e);
                        }
                    }
                    img = resizeImageWithHintKeepAspect(img, 0, PROFILE_PIC_HEIGHT_IN_PIX);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {

                        ImageIO.write(img, fileExtension, os);

                    } catch (IOException ex) {

                        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, write image  error!!", ex);

                    }

                    ci.setImage(os.toByteArray());
                    ci.setImageType(imgType);
                    ci.setCustomers(cust);
                    ci.setCustomerId(cust);
                    ci.setDatetaken(new Date());

                    ejbFacade.edit(ci);
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

    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected();
    }

    public CustomerImages getSelected() {
        if (current == null) {
            setCurrent(new CustomerImages());
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(CustomerImages selected) {
        this.current = selected;
    }

    private void createGallery(int customerId) {
        images = getFacade().findAllByCustId(customerId, true);
        for (int i = images.size() - 1; i >= 0; i--) {
            CustomerImages ci = images.get(i);
            if (ci.getImage() == null) {
                images.remove(i);
            }
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
        String name = fileName.substring(0, fileName.lastIndexOf(".")).toLowerCase();
        try {
            uploadedImageTempFile = File.createTempFile(name, fileExtension);
        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "Create Temp File", ex);
        }

        /*   try (FileImageOutputStream imageOutput = new FileImageOutputStream(uploadedImageTempFile)) {
         byte[] image = IOUtils.toByteArray(file.getInputstream());
         imageOutput.write(image, 0, image.length);

         } catch (IOException iOException) {
         Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, iOException);
         }*/
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
            logger.log(Level.WARNING, "processUploadedFile , Cannot add default profile pic  due the picture not being in jpeg, gif or png. resource:{0}", new Object[]{fileName});
            return;
        }
        logger.log(Level.INFO, "processUploadedFile , Name of Uploaded File: {0}, contentType: {1}, file Extension:{2}", new Object[]{fileName, contentType, fileExtension});
        try {
            img = ImageIO.read(file.getInputstream());
            ImageIO.write(img, fileExtension, uploadedImageTempFile);
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Loading image into buffer error!!");
        }

        //BufferedImage scaledImg = resizeImageKeepAspect(img, new_width);
        BufferedImage scaledImg = resizeImageWithHintKeepAspect(img, 0, new_height);// use a 0 for heigh or width to keep aspect
        updateImages(scaledImg, fileExtension);
        current.setImageType(imgType);//jpeg
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
                            current.setDatetaken(dateThePhotoWasTaken);
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

    }

    public void handleFileUpload(FileUploadEvent event) {
//Barefoot-image_100_by_100.jpg

        uploadedFile = event.getFile();
        processUploadedFile(uploadedFile);
        setSaveButtonDisabled(false);
        //RequestContext.getCurrentInstance().update("createCustomerForm");
    }

    private void updateImages(BufferedImage img, String fileType) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {

            ImageIO.write(img, fileType, os);

        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Scaling image  error!!");
        }
        getSelected().setImage(os.toByteArray());
        InputStream stream = new ByteArrayInputStream(os.toByteArray());
        try {

            setUploadedImage(new DefaultStreamedContent(stream, "image/jpeg"));
            //setCroppedImage(getUploadedImage());
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Update image error!!");
        }

    }

    private BufferedImage rotateImage(int degrees, BufferedImage oldImage) {

        BufferedImage newImage = new BufferedImage(oldImage.getHeight(), oldImage.getWidth(), oldImage.getType());
        Graphics2D graphics = (Graphics2D) newImage.getGraphics();
        graphics.rotate(Math.toRadians(degrees), newImage.getWidth() / 2, newImage.getHeight() / 2);
        graphics.translate((newImage.getWidth() - oldImage.getWidth()) / 2, (newImage.getHeight() - oldImage.getHeight()) / 2);
        graphics.drawImage(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), null);

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
        boolean inRole = false;
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
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
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
        int newHeight = new_height;

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
        updateImages(scaledImg, type);
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("IMageRotateSuccessful"));
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
        setCurrent(new CustomerImages());
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
        if (uploadedFile != null) {
            processUploadedFile(uploadedFile);
            createFromDialogue();
            logger.log(Level.INFO, "Photo upload successfully from mobile device.");
        } else {
            logger.log(Level.WARNING, "The uploaded photo is NULL. Photo upload from mobile device failed.");
        }

    }

    public void createFromDialogue() {
        /*  if (croppedImage != null) {
         BufferedImage img = null;
         try {
         img = ImageIO.read(croppedImage.getStream());
         } catch (IOException e) {
         JsfUtil.addErrorMessage(e, "Loading image into buffer error!!");
         }

         ByteArrayOutputStream os = new ByteArrayOutputStream();
         try {
         ImageIO.write(img, "jpeg", os);

         } catch (IOException ex) {
         Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);

         }

         current.setImage(os.toByteArray());

         }*/
        create();
        clearPhoto();
        recreateModel();
    }

    public String create() {
        try {
            current.setId(0);// auto generated by DB , but cannot be null 
            Customers cust = getSelectedCustomer();
            if (current.getCustomerId() == null) {
                current.setCustomerId(cust);
            }

            getFacade().create(current);
            if (isProfilePhoto()) {
                cust.setProfileImage(current);
                ejbCustomersFacade.edit(cust);
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                custController.setSelected(cust);
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomerImagesCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
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

    public String crop() {

        InputStream stream = new ByteArrayInputStream(croppedImage.getBytes());
        try {
            String extension = getFileExtensionFromFilePath(croppedImage.getOriginalFilename());
            setUploadedImage(new DefaultStreamedContent(stream, "image/" + extension)
            );
            //setCroppedImage(getUploadedImage());

            FileImageOutputStream imageOutput;
            try {
                imageOutput = new FileImageOutputStream(new File(uploadedImageTempFile.getAbsolutePath()));
                imageOutput.write(croppedImage.getBytes(), 0, croppedImage.getBytes().length);
                imageOutput.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Update image error!!");
        }

        return null;
    }

    private void loadImage(CustomerImages ci) {
        String placeholderImage = "/resources/images/Barefoot-image_100_by_100.jpg";
        uploadedImage = loadDefaultImageIfNull(ci, placeholderImage);

    }

    private StreamedContent loadDefaultImageIfNull(CustomerImages ci, String defaultImagePath) {
        StreamedContent sc = null;
        if (ci.getImage() == null) {
            try {
                // get default image
                InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(defaultImagePath);
                sc = new DefaultStreamedContent(stream, "image/jpeg");
            } catch (Exception e) {
                String message = "Could not load default image:" + defaultImagePath;
                JsfUtil.addErrorMessage(message);
                logger.log(Level.WARNING, message, e);
            }
        } else {
            ByteArrayInputStream is = new ByteArrayInputStream(ci.getImage());
            String imgtyp = getImageTypeString(ci.getImageType());
            sc = new DefaultStreamedContent(is, imgtyp);
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

    public String destroy() {
        setCurrent((CustomerImages) getItems().getRowData());
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public void destroyListener() {
        setCurrent((CustomerImages) getItems().getRowData());
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
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

    /**
     * @return the uploadedImage
     */
    public StreamedContent getUploadedImage() {
        if (uploadedImage == null) {
            try {
                // get default image 
                if(current == null){
                    setCurrent(new CustomerImages());
                }
                loadImage(current);
                //InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                //uploadedImage = new DefaultStreamedContent(stream, "image/jpeg");
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
            }
        }
        return uploadedImage;
    }

    public String getDynamicTitle() {
        String title = "";
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imagetitleid");
        if (id != null) {
            Integer imageId = Integer.parseInt(id);
            CustomerImages ci = ejbFacade.find(imageId);
            title = ci.getCustomerId().getFirstname() + " " + ci.getCustomerId().getLastname();
        }
        return title;
    }

    public String getDynamicDescription() {
        String desc = "";
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imagedescid");
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
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("imageid");

        if (id != null) {
            Integer imageId = Integer.parseInt(id);
            CustomerImages ci = ejbFacade.find(imageId);
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
    public void setUploadedImage(StreamedContent uploadedImage) {
        this.uploadedImage = uploadedImage;

    }

    /**
     * @return the images
     */
    public List<CustomerImages> getImages() {
        if (images == null) {
            createGallery(getUser());
            if(images == null){
                images = new ArrayList<>();
            }
        }

        return images;
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
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

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

    @FacesConverter(forClass = CustomerImages.class)
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
