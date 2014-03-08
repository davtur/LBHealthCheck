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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
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
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@Named("customerImagesController")
@SessionScoped
public class CustomerImagesController implements Serializable {

    private CustomerImages current;
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

    public CustomerImagesController() {
    }

    @PostConstruct
    public void init() {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Customers cust = ejbCustomersFacade.findCustomerByUsername(loggedInUser);
        createGallery(cust.getId());
    }

    private int getUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        ChartController cc = (ChartController) context.getApplication().evaluateExpressionGet(context, "#{chartController}", ChartController.class);
        return cc.getUser();

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

    private static BufferedImage resizeImageWithHintKeepAspect(BufferedImage originalImage, int newWidth) {
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        float w = originalImage.getWidth();
        float h = originalImage.getHeight();
        float newHeight = (new Float(newWidth) / w) * h;
        int nheight = Math.round(newHeight);
        BufferedImage resizedImage = new BufferedImage(newWidth, nheight, type);
        Graphics2D g = resizedImage.createGraphics();

        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, newWidth, nheight, null);
        g.dispose();
        return resizedImage;
    }

    private static BufferedImage resizeImageKeepAspect(BufferedImage originalImage, int newWidth) {
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

        float w = originalImage.getWidth();
        float h = originalImage.getHeight();
        float newHeight = (new Float(newWidth) / w) * h;
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

    public void handleFileUpload(FileUploadEvent event) {
//Barefoot-image_100_by_100.jpg
        int new_width = 800;
        UploadedFile file = event.getFile();
        BufferedImage img = null;
        try {
            img = ImageIO.read(file.getInputstream());
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Loading image into buffer error!!");
        }
        InputStream stream = null;

        //BufferedImage scaledImg = resizeImageKeepAspect(img, new_width);
        BufferedImage scaledImg = resizeImageWithHintKeepAspect(img, new_width);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(scaledImg, "jpeg", os);

        } catch (IOException ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Scaling image  error!!");
        }

        current.setImage(os.toByteArray());
        current.setImageType(2);
        stream = new ByteArrayInputStream(os.toByteArray());

        //BufferedImage data = null;
        //Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");
        // ImageReader reader = (ImageReader) readers.next();
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
                        current.setDatetaken(dateThePhotoWasTaken);
                    } else {
                        JsfUtil.addErrorMessage("Couldnt get the date the photo was taken from the image file!. The Date is Null!");
                    }
                    JsfUtil.addSuccessMessage("Modifying the photo taken date to the date extracted from the photo: " + dateString);

                }

            } else {
                JsfUtil.addErrorMessage("Couldnt get the date the photo was taken from the image file! No EXIF data in the photo.");
            }
            /* ImageInputStream iis = ImageIO.createImageInputStream(file.getInputstream());
             reader.setInput(iis, true);
             IIOMetadata tags = reader.getImageMetadata(0);
             Node tagNode = tags.getAsTree(tags.getNativeMetadataFormatName());
             String st = tagNode.getTextContent();
            
             data = reader.read(0);*/
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Couldnt get the date the photo was taken from the image file! No EXIF data in the photo.");
        }

        try {

            setUploadedImage(new DefaultStreamedContent(stream, "image/jpeg"));
        } catch (Exception ex) {
            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Uploading image error!!");
        }
        setSaveButtonDisabled(false);
        /* try {
         getFacade().edit(current);
         JsfUtil.addSuccessMessage(configMapFacade.getConfig("PhotoUploaded"));
        
         } catch (Exception e) {
         JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        
         }*/
//application code
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

    public String prepareCreate() {
        clearPhoto();
        return "Create";
    }

    private void clearPhoto() {
        setCurrent(new CustomerImages());
        loadImage(current);
        selectedItemIndex = -1;
        setSaveButtonDisabled(true);
    }

    public String prepareEdit() {
        // setCurrent((CustomerImages) getItems().getRowData());
        loadImage(current);
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void createFromDialogue() {
        create();
        clearPhoto();
        recreateModel();
    }

    public String create() {
        try {
            current.setId(0); // auto generated by DB , but cannot be null    \n
            getFacade().create(current);
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

    private void loadImage(CustomerImages ci) {
        if (ci.getImage() == null) {
            try {
                // get default image
                InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                uploadedImage = new DefaultStreamedContent(stream, "image/jpeg");
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Trying to set Barefoot-image_100_by_100.jpg as the defailt image failed!");
            }
        } else {
            ByteArrayInputStream is = new ByteArrayInputStream(ci.getImage());
            String imgtyp = getImageTypeString(ci.getImageType());
            uploadedImage = new DefaultStreamedContent(is, imgtyp);
        }
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

    private void recreateModel() {
        items = null;
        filteredItems = null;
       
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
                InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream("/resources/images/Barefoot-image_100_by_100.jpg");
                uploadedImage = new DefaultStreamedContent(stream, "image/jpeg");
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

        createGallery(getUser());

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
