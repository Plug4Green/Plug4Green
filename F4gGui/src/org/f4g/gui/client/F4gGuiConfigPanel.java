package org.f4g.gui.client;

import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;


public class F4gGuiConfigPanel extends LayoutPanel {
	public enum VIEW {VIEW_TEXT, VIEW_XML};
	
    final VerticalPanel panelConfigLeft = new VerticalPanel();
    final VerticalPanel panelConfigRight = new VerticalPanel();
	final ListBox lstDirectories = new ListBox();
	final ListBox lstFiles = new ListBox();
	final Label lblConfigInfo = new Label();
	final Label lblConfigFilename = new Label();
    final Button btnNew = new Button("New File");
    final Button btnNewMetaModel = new Button("New MetaModel");
    final Button btnNewSLA = new Button("New SLA");
    final Button btnNewWorkload = new Button("New Workload");
    final Button btnSave = new Button("Save");
    final Button btnRename = new Button("Rename");
    final Button btnClose = new Button("Close");
    final Button btnDelete = new Button("Delete");
    final Button btnSmaller = new Button("-");
    final Button btnLarger = new Button("+");
    final RadioButton radioText = new RadioButton("editmode", "Plain text");
    final RadioButton radioXml = new RadioButton("editmode", "XML");
    final Label lblXmlWarning = new Label();
    final Label lblXmlSchema = new Label();
	final TextArea txtEditor = new TextArea();
	final SimplePanel panelXmlEditor = new SimplePanel();
	private String filename = "";
	private String content = "";
	private int editorVerticalExtra = 0; // extra large vertical size
	// configChanged is True if a config file is changed and need to be saved

	// xml editor will be loaded as soon as the users requests the xml view
	private XmlEditor xmlEditor = null;  
	
	VIEW view = VIEW.VIEW_TEXT;
	String directory = "";
	
	/**
	 * Create a remote service proxy to talk to the server-side ConfigService 
	 * service.
	 */
	private final ConfigServiceAsync configService = 
		GWT.create(ConfigService.class);

	/**
	 * Constructor
	 */
	F4gGuiConfigPanel() {
		create();
	}
	
	/**
	 * Create the panel where the configuration files can be displayed and edited
	 * @return
	 */
	private void create() {
	    // create a main panel, and a left and right panel
	    //
		FlowPanel main = new FlowPanel();
	    main.setStyleName("panelTabContent");	    
	    this.add(main);
	    
	    HorizontalPanel hPanel = new HorizontalPanel();
	    main.add(hPanel);	    
	    panelConfigLeft.setStyleName("configPanelLeft");
	    hPanel.add(panelConfigLeft);
	    panelConfigRight.setStyleName("configPanelRight");
	    hPanel.add(panelConfigRight);
	    panelConfigRight.setVisible(false);	    
	    
	    // create the header for the left panel
	    Label headerFiles = new Label("Files");
	    headerFiles.setStyleName("h2 top");
	    panelConfigLeft.add(headerFiles);	    

	    // create a selection box with the available directories
	    panelConfigLeft.add(lstDirectories);
	    lstDirectories.setStyleName("configDirectories");
	    lstDirectories.addChangeHandler(
	    		new ChangeHandler() {
					@Override
					public void onChange(ChangeEvent event) {
						int index = lstDirectories.getSelectedIndex();
						String title = lstDirectories.getItemText(index);
						
						setDirectoryTitle(title);
					}
	    		}
	    	);
	    
	    // create the list with config files
	    panelConfigLeft.add(lstFiles);
	    lstFiles.setStyleName("configFileList");
	    lstFiles.setVisibleItemCount(10);
	    lstFiles.setWidth("200px");
	    lstFiles.addChangeHandler(
	    		new ChangeHandler() {
					@Override
					public void onChange(ChangeEvent event) {
						int index = lstFiles.getSelectedIndex();
						if (index >= 0) {
							String filename = lstFiles.getItemText(index);
							loadFile(filename);
						}
					}
	    		}
	    	);
	    
	    // retrieve all directories
	    retrieveDirectoryTitles();
	    
	    // create the header for the new buttons
	    Label newFiles = new Label("New");
	    newFiles.setStyleName("h2");
	    panelConfigLeft.add(newFiles);	    
	    
	    // create the new file button
	    panelConfigLeft.add(btnNew);
	    btnNew.setStyleName("newButton");
	    btnNew.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						String newFilename = Window.prompt("New file:", ""); 
						if (newFilename == null)
							return;						
						
						String initialContent = "";
						newFile(newFilename, initialContent );
					}
				}	
			);
	    
	    // create the new meta model button
	    panelConfigLeft.add(btnNewMetaModel);
	    btnNewMetaModel.setStyleName("newButton");
	    btnNewMetaModel.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						String newFilename = Window.prompt("New file:", ""); 
						if (newFilename == null)
							return;						
						
						String initialContent = 
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
							"<FIT4Green \n" +
							" xmlns=\"http://www.fit4green.eu/schemes/MetaModel\" \n" +
							" xmlns:x0=\"http://www.w3.org/2001/XMLSchema\"/>\n";
						newFile(newFilename, initialContent);
					}
				}	
			);	    
	    
	    // create the new SLA model button
	    panelConfigLeft.add(btnNewSLA);
	    btnNewSLA.setStyleName("newButton");
	    btnNewSLA.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						String newFilename = Window.prompt("New file:", ""); 
						if (newFilename == null)
							return;						

						String initialContent = 
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
							"<sla:FIT4GreenSLA \n" +
							" xmlns:sla=\"http://www.fit4green.eu/schemes/SLA\" \n" +
							" xmlns:x0=\"http://www.w3.org/2001/XMLSchema\" />\n";
						newFile(newFilename, initialContent);
					}
				}	
			);	    
	    
	    // create the new SLA model button
	    panelConfigLeft.add(btnNewWorkload);
	    btnNewWorkload.setStyleName("newButton");
	    btnNewWorkload.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						String newFilename = Window.prompt("New file:", ""); 
						if (newFilename == null)
							return;						

						String initialContent = 
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
							"<wlc:Workload \n" +
							" xmlns:wlc=\"http://www.fit4green.eu/schemes/Workload\" \n" +
							" xmlns:x0=\"http://www.w3.org/2001/XMLSchema\"/>\n";
						newFile(newFilename, initialContent);
					}
				}	
			);
	    
	    // create info box
	    // TODO: place the info on a handier, more central location
	    panelConfigLeft.add(lblConfigInfo);
	    
	    // create the header for the right panel
	    lblConfigFilename.setStyleName("h2 top");
	    panelConfigRight.add(lblConfigFilename);

	    // create the main menu, which consists of two groups of buttons: 
	    // Edit, and Resize
	    HorizontalPanel panelMenu = new HorizontalPanel();
	    panelConfigRight.add(panelMenu);
	    panelMenu.setWidth("100%");	    
	    
	    // create the save, close, rename, delete buttons
	    HorizontalPanel panelEdit = new HorizontalPanel();
	    panelMenu.add(panelEdit);
	    panelEdit.add(btnSave);
	    btnSave.setStyleName("actionButton");
	    btnSave.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					saveFile();
				}
			}	
		);
	    panelEdit.add(btnRename);
	    btnRename.setStyleName("actionButton");
	    btnRename.addClickHandler(	
				new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						String newFilename = 
							Window.prompt("Rename file to:", filename); 
						if (newFilename == null)
							return;

						renameFile(filename, newFilename);
					}
				}	
			);	 	    
	    panelEdit.add(btnClose);
	    btnClose.setStyleName("actionButton");
	    btnClose.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					closeFile();
				}
			}	
		);			   
	    panelEdit.add(btnDelete);
	    btnDelete.setStyleName("actionButton");
	    btnDelete.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					boolean yes = Window.confirm(
							"Are you sure you want to delete the file '" +
							lblConfigFilename.getText() + "'?\n\n" +
							"This action cannot be undone.");
					
					if (yes) {
						// actually delete the file
						deleteFile(lblConfigFilename.getText());
					}
				}
			}	
		);			
		
	    // create mode buttons
	    panelEdit.add(radioText);
		radioText.setStyleName("configRadioEditor");
		radioText.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					setView(VIEW.VIEW_TEXT);
				}
			});
		panelEdit.add(radioXml);
		radioXml.setStyleName("configRadioEditor");
		radioXml.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (isViewSupported(VIEW.VIEW_XML, getText())) {
					setView(VIEW.VIEW_XML);
				}
				else {
					Window.alert("Cannot switch to XML editor,\n" +
								 "No known XML schema found.");
					setView(VIEW.VIEW_TEXT);
				}
			}
		});
		
	    // create buttons smaller and larger
		HorizontalPanel panelResize = new HorizontalPanel();
		panelMenu.add(panelResize);
	    panelMenu.setCellHorizontalAlignment(panelResize, 
	    		HorizontalPanel.ALIGN_RIGHT);
		panelResize.add(btnSmaller);
	    btnSmaller.setStyleName("actionButton");
	    btnSmaller.setTitle("Reduce the vertical size of the editor");
	    btnSmaller.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					editorVerticalExtra -= 50;
					if (editorVerticalExtra < 0) editorVerticalExtra = 0;
					onResize();
				}
			}	
		);	
	    btnSmaller.setVisible(false);
	    panelResize.add(btnLarger);
	    btnLarger.setStyleName("actionButton");
	    btnLarger.setTitle("Enlarge the vertical size of the editor");
	    btnLarger.addClickHandler(	
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					editorVerticalExtra += 50;
					if (editorVerticalExtra > 1000) editorVerticalExtra = 1000;
					onResize();
				}
			}	
		);	
	    btnLarger.setVisible(false);

		// create a warning for the xml editor
		lblXmlWarning.setText(
				"Warning: Do not use the Refresh and Save button from " +
				"inside the XML Editor!");
		lblXmlWarning.setStyleName("configXmlWarning");
		panelConfigRight.add(lblXmlWarning);
		
	    // Create the textarea for the contents of the configuration files
		panelConfigRight.add(txtEditor);
		txtEditor.setText("");	
		txtEditor.setStyleName("configFile");	    
		// Add handlers to check when the contents of the file changed by user
		txtEditor.addChangeHandler(
	    		new ChangeHandler() {
					@Override
					public void onChange(ChangeEvent event) {
						enableConfigButtons(true);
					}
	    		}
	    	);
		txtEditor.addKeyPressHandler(
	    		new KeyPressHandler() {
					@Override
					public void onKeyPress(KeyPressEvent event) {
						enableConfigButtons(true);
					}
	    		});
		
		// Create a panel where we can create the xml editor
		panelConfigRight.add(panelXmlEditor);

		// create an information lable which can show the current schema
		panelConfigRight.add(lblXmlSchema);
		lblXmlSchema.setStyleName("info");
		
	    setView(VIEW.VIEW_TEXT);
	}
	
	@Override 
	public void onResize() {
		// This is an ugly but working hack to adjust the width and height of 
		// the text editor. Better solution: build this config panel using
		// a DockLayoutPanel.
		int panelHeight = this.getOffsetHeight();
		int panelWidth = this.getOffsetWidth();
		int marginWidth = 60;
		int marginHeight = 115;
		
		int panelLeftWidth = panelConfigLeft.getOffsetWidth();
		
		switch(view) {
		case VIEW_TEXT:
			int txtHeight = Math.max(panelHeight - marginHeight, 0);
			int txtWidth = Math.max(panelWidth - panelLeftWidth - marginWidth, 0);
			txtEditor.setHeight(txtHeight + "px");
			txtEditor.setWidth(txtWidth + "px");
			break;

		case VIEW_XML:
			if (xmlEditor != null) {
				int xmlHeight = Math.max(panelHeight - marginHeight + 
						editorVerticalExtra - 40, 0);
				int xmlWidth = Math.max(panelWidth - panelLeftWidth - marginWidth, 0);
				xmlEditor.setHeight(xmlHeight + "px");
				xmlEditor.setWidth(xmlWidth + "px");
			}
			break;
		}
	}
	
	/**
	 * Check if a view is supported by the file.
	 * @param view    A view
	 * @param content The contents to be checked
	 */
	public boolean isViewSupported(VIEW view, String content) {
		if (view == VIEW.VIEW_XML) {
			// check if the current file contains a known schema
			// if not, change to text view
			String schema = XmlEditor.getXmlSchema(content);
			if (schema.isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Set a view mode, choose from PLAINTEXT or XML
	 * @param view
	 */
	public void setView (VIEW view) {
		// get content from the current editor
		String contentCurrent = getText();

		this.view = view;
		
		switch (this.view) {
			case VIEW_TEXT:
				radioText.setValue(true);
				txtEditor.setVisible(true);
				
				btnSmaller.setVisible(false);
				btnLarger.setVisible(false);				
				lblXmlWarning.setVisible(false);
				
				break;

			case VIEW_XML:
				radioXml.setValue(true);
				txtEditor.setVisible(false);
				
				btnSmaller.setVisible(true);
				btnLarger.setVisible(true);
				lblXmlWarning.setVisible(true);

				break;
		}

		// put the content in the newly chosen editor
		setText(contentCurrent);
		
		// perform a resize
		onResize();
	}
	
	/**
	 * Set a directory title
	 * @param directoryTitle
	 */
	public void setDirectoryTitle(String directoryTitle) {
		// first close and save any opened file
		if (needToSaveConfigFile()) {
			Callback callback = new Callback() {
				@Override
				void onDone(Object obj) {
					closeFile();
				}

				@Override
				void onError(Throwable caught) {
				}
			};
			
			saveFile(callback);
		} else {
			closeFile();
		}						
		
		directory = directoryTitle;
		
		// retrieve the new files
		retrieveFileList(directory);
	}
	
	/**
	 * 
	 * @return view
	 */
	public VIEW getView() {
		return view;
	}
	
	/**
	 * Enable or disable the config buttons
	 * @param enable
	 */
	void enableConfigButtons(boolean enable) {
		lstDirectories.setEnabled(enable);
		lstFiles.setEnabled(enable);

		btnNew.setEnabled(enable);
	    btnNewMetaModel.setEnabled(enable);
	    btnNewSLA.setEnabled(enable);
		btnSave.setEnabled(enable);
		btnClose.setEnabled(enable);
		btnRename.setEnabled(enable);
		btnDelete.setEnabled(enable);
		txtEditor.setEnabled(enable);
	}

	/**
	 * Retrieve the a list with the configuration files via an asynchronous 
	 * callback, and update the list in the GUI
	 */
	private void retrieveDirectoryTitles() {
		configService.getDirectoryTitles(
			new AsyncCallback< Vector<String> >() {
				public void onSuccess(Vector<String> directoryTitles) {
					updateDirectoryTitles(directoryTitles);
					
				    // set the directory to the currently selected title
					int index = lstDirectories.getSelectedIndex();
					if (index != -1) {
						String title = lstDirectories.getItemText(index);
						setDirectoryTitle(title);
					}
				}

				public void onFailure(Throwable caught) {
					lblConfigInfo.setText("Error: " + caught.getMessage());
					lblConfigInfo.setStyleName("info-error");	
				}
			});
	}
		
	
	/**
	 * Retrieve the a list with the configuration files via an asynchronous 
	 * callback, and update the list in the GUI
	 */
	private void retrieveFileList(String directoryTitle) {
		configService.getFileList(directoryTitle, 
			new AsyncCallback< Vector<String> >() {
				public void onSuccess(Vector<String> configFiles) {
					updateFileList(configFiles);
				}

				public void onFailure(Throwable caught) {
					lblConfigInfo.setText("Error: " + caught.getMessage());
					lblConfigInfo.setStyleName("info-error");	
				}
			});
	}

	/**
	 * Load a configuration file
	 * If the current file is changed, the file is saved first
	 */
	private void loadFile(String filename) {
		// check if the current file needs to be saved
		if (needToSaveConfigFile()) { 
			saveAndLoadFile(filename);
			return;
		}
		
		lblConfigInfo.setText("Loading file '" + filename + "'...");
		lblConfigInfo.setStyleName("info");		
		enableConfigButtons(false);
		
		class LoadCallback implements AsyncCallback<String> {
			public void onSuccess(String content) {
				lblConfigInfo.setText("");
				lblConfigInfo.setStyleName("info");		
				enableConfigButtons(true);
				showConfigFile(filename, content);
			}

			public void onFailure(Throwable caught) {
				lblConfigInfo.setText("Error: " + caught.getMessage());
				lblConfigInfo.setStyleName("info-error");		
				enableConfigButtons(true);
			}
			
			public String filename = "";
		}
		
		LoadCallback callback = new LoadCallback();
		callback.filename = filename;
		
		configService.getFile(directory, filename, callback);
	}

	/**
	 * @class Callback
	 * A simple callback class used for the method saveFile
	 */
	abstract class Callback {
		abstract void onDone(Object obj);
		abstract void onError(Throwable caught);
	}
	
	/**
	 * Save the current configuration file
	 */ 	
	private void saveFile() {
		saveFile(null);
	}
	
	/**
	 * Save the current file, and on success, close it
	 */
	private void saveAndCloseFile() {
		class CloseCallback extends Callback {
			@Override
			void onDone(Object obj) {
				closeFile();
			}
	
			@Override
			void onError(Throwable caught) {
			}
		};
		
		CloseCallback callback = new CloseCallback();
		saveFile(callback);	
	}
	
	/**
	 * Save the current file, and on success, close it
	 */
	private void saveAndLoadFile(String filename) {
		class LoadCallback extends Callback {
			LoadCallback (String filename) {
				this.filename = filename;
			}
			
			@Override
			void onDone(Object obj) {
				loadFile(filename);
			}
	
			@Override
			void onError(Throwable caught) {
			}
			
			private String filename;
		};
		
		LoadCallback callback = new LoadCallback(filename);
		saveFile(callback);	
	}	
	
	
	/**
	 * Save the current file, and on success, close it
	 * @param filename        The name for the new file
	 * @param initialContent  The initial content for the new file
	 */
	private void saveAndNewFile(String filename, String initialContent) {
		class NewCallback extends Callback {
			NewCallback (String filename, String initialContent) {
				this.filename = filename;
				this.initialContent = initialContent;
			}
			
			@Override
			void onDone(Object obj) {
				newFile(filename, initialContent);
			}
	
			@Override
			void onError(Throwable caught) {
			}
			
			private String filename;
			private String initialContent;
		};
		
		NewCallback callback = new NewCallback(filename, initialContent);
		saveFile(callback);	
	}	
		
	
	/**
	 * Save the current configuration file
	 * @param callback   Method to be executed on callback
	 */
	private void saveFile(Callback callback) {
		content = getText();
		lblConfigInfo.setText("Saving file '" + filename + "'...");
		lblConfigInfo.setStyleName("info");
		enableConfigButtons(false);
		
		class SaveCallback implements AsyncCallback<String> {
			private Callback callback = null;

			/** set a callback function which will be executed 
			 * 
			 * @param callback
			 */
			public void setCallback(Callback callback) {
				this.callback = callback;
			}
			
			public void onSuccess(String content) {
				lblConfigInfo.setText("");
				lblConfigInfo.setStyleName("info");
				enableConfigButtons(true);
				
				// on callback, it is possible to close the current file,
				// or to load another file.
				if (callback != null) {
					callback.onDone(null);
				}
			}

			public void onFailure(Throwable caught) {
				lblConfigInfo.setText("Error: " + caught.getMessage());
				lblConfigInfo.setStyleName("info-error");
				enableConfigButtons(true);
				
				// on callback, it is possible to close the current file,
				// or to load another file.
				if (callback != null) {
					callback.onError(caught);
				}				
			}
		};
		
		SaveCallback saveCallback = new SaveCallback();
		saveCallback.setCallback(callback);
		configService.setFile(directory, filename, content, saveCallback);
	}


	/**
	 * Create a new configuration file
	 * It the current file is changed, it will be saved first
	 * @param initialContent
	 */
	private void newFile(String newFilename, String initialContent) {
		if (needToSaveConfigFile()) { 
			saveAndNewFile(newFilename, initialContent);
			return;
		}

		lblConfigInfo.setText("Creating file '" + newFilename + "'...");
		lblConfigInfo.setStyleName("info");		
		enableConfigButtons(false);
		
		class NewFileCallback implements AsyncCallback<String> {
			public void onSuccess(String content) {
				lblConfigInfo.setText("");
				lblConfigInfo.setStyleName("info");		
				enableConfigButtons(true);

				showConfigFile(newFilename, initialContent);				

				retrieveFileList(directory);
			}

			public void onFailure(Throwable caught) {
				lblConfigInfo.setText("Error: " + caught.getMessage());
				lblConfigInfo.setStyleName("info-error");		
				enableConfigButtons(true);
			}
			
			public String newFilename = "";
			public String initialContent = "";
		}
		
		NewFileCallback callback = new NewFileCallback();
		callback.newFilename = newFilename;
		callback.initialContent = initialContent;
		
		configService.newFile(directory, newFilename, initialContent, callback);
	}
	
	/**
	 * Rename a file
	 * @param oldFilename
	 * @param newFilename
	 */
	private void renameFile(String oldFilename, String newFilename) {
		lblConfigInfo.setText(
				"Renaming file '" + oldFilename + "' to ' + " + newFilename + "'...");
		lblConfigInfo.setStyleName("info");	
		enableConfigButtons(false);
		
		class RenameCallback implements AsyncCallback<String> {
			public void onSuccess(String content) {
				lblConfigInfo.setText("");
				lblConfigInfo.setStyleName("info");		
				enableConfigButtons(true);

				retrieveFileList(directory);

				filename = newFilename;
				lblConfigFilename.setText(filename); 
			}

			public void onFailure(Throwable caught) {
				lblConfigInfo.setText("Error: " + caught.getMessage());
				lblConfigInfo.setStyleName("info-error");		
				enableConfigButtons(true);
			}
			
			public String newFilename = "";
		}
		
		RenameCallback callback = new RenameCallback();
		callback.newFilename = newFilename;
		
		configService.renameFile(directory, oldFilename, newFilename, callback);
	}
	
	/**
	 * Load a configuration file
	 */
	private void deleteFile(String filename) {
		lblConfigInfo.setText("Deleting file '" + filename + "'...");
		lblConfigInfo.setStyleName("info");		
		enableConfigButtons(false);
		
		class DeleteCallback implements AsyncCallback<String> {
			public void onSuccess(String content) {
				lblConfigInfo.setText("");
				lblConfigInfo.setStyleName("info");		
				enableConfigButtons(true);
				closeFile();
				retrieveFileList(directory);
			}

			public void onFailure(Throwable caught) {
				lblConfigInfo.setText("Error: " + caught.getMessage());
				lblConfigInfo.setStyleName("info-error");		
				enableConfigButtons(true);
			}
		}
		
		DeleteCallback callback = new DeleteCallback();
		
		configService.deleteFile(directory, filename, callback);
	}

	/**
	 * Show a configuration on screen, where it can be edited
	 * @param filename
	 * @param content
	 */
	void showConfigFile(String filename, String content) {
		this.filename = filename;
		this.content = content;

		
		boolean showViewMode = isViewSupported(VIEW.VIEW_XML, this.content);
		enableViewMode(showViewMode);
		
		if (!isViewSupported(this.view, this.content)) {
			// change to text view
			setView(VIEW.VIEW_TEXT);
		}
		
		// perform a resize
		onResize();		
		
		lblConfigFilename.setText(filename); 
		
		panelConfigRight.setVisible(true);
		setText(content);
	}

	/**
	 * enable or disable the view mode: the radiobuttons which allow to choose
	 * text or xml mode.
	 * @param enable
	 */
	void enableViewMode(boolean enable) {
		radioText.setVisible(enable);
		radioXml.setVisible(enable);
	}
	
	/**
	 * Close the panel with the file
	 * If the file is changed, it will be saved.
	 */
	void closeFile() {
		if (needToSaveConfigFile()) {
			saveAndCloseFile();
		}
		
		panelConfigRight.setVisible(false);
		if (xmlEditor != null) {
			panelXmlEditor.remove(xmlEditor);
			xmlEditor = null;
		}
		
		int index = lstFiles.getSelectedIndex();
		if (index >= 0) {
			lstFiles.setItemSelected(index, false);
		}
	    
		lblConfigFilename.setText("");	
		txtEditor.setText("");	
	    
		this.filename = "";
		this.content = "";
	}
	
	/**
	 * Check if the currently opened config file needs to be saved.
	 * This is the case if the contents has been changed AND if the user 
	 * confirms that he want to save the file
	 * @return saving   True if the file needs to be saved
	 */
	boolean needToSaveConfigFile() {
		if (!content.equals(getText())) {
			boolean doSave = Window.confirm(
						"Contents of the configuration file are changed.\n\n" +
						"Do you want to save the file?");
			if (doSave) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Retrieve the current text in the editor (can be the plain text
	 * editor or the xml editor)
	 * @return
	 */
	private String getText() {
		switch (getView()) {
			case VIEW_TEXT: 
				return txtEditor.getText();
				
			case VIEW_XML:
				String text = "";
				if (xmlEditor != null) {
					text = xmlEditor.getText();
					
					if (text.isEmpty()) {
						Window.alert(
								"The XML editor does not return any text.\n" +
								"Probably the XML file resulted in errors or " +
								"the XML editor dit not load correctly.\n\n" +
								"Reverting to the last saved contents...");

						text = content;
					}					
				}

				return text;
		}
		
		return "";
	}
	
	/**
	 *Set text in the editor (can be the plain text editor or the xml editor)
	 * @return
	 */	
	private void setText(String text) {
		switch (getView()) {
			case VIEW_TEXT: 
				txtEditor.setText(text);
				
				// unload the xml editor if still loaded
				if (xmlEditor != null) {
					panelXmlEditor.remove(xmlEditor);
					xmlEditor = null;
				}
				lblXmlSchema.setText("");
				
				break;
			case VIEW_XML:
				if (xmlEditor == null) {
					xmlEditor = new XmlEditor(text);
					panelXmlEditor.add(xmlEditor);
				}
				else {
					xmlEditor.setText(text);
				}
				lblXmlSchema.setText("Current schema: " + 
						XmlEditor.getXmlSchema(text));

				break;
		}
	}
	
	/**
	 * Update the list with directory titles
	 * @param directoryTitles
	 */
	void updateDirectoryTitles(Vector<String> directoryTitles){
		lstDirectories.clear();

	    for (String title : directoryTitles) {
	    	lstDirectories.addItem(title);
	    }
	}
	
	/**
	 * Update the list with configuration files
	 * @param configFiles
	 */
	void updateFileList(Vector<String> configFiles){
		lstFiles.clear();

	    for (String configFile : configFiles) {
	    	lstFiles.addItem(configFile);
	    }
	}	
}